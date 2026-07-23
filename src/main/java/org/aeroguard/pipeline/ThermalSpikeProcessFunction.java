package org.aeroguard.pipeline;

import org.aeroguard.model.Alert;
import org.aeroguard.model.AssetEvent;
import org.aeroguard.model.AssetOperatingModeEvent;
import org.aeroguard.model.OperatingMode;
import org.aeroguard.model.Telemetry;
import org.aeroguard.model.ThresholdConfig;
import org.apache.flink.api.common.state.ListState;
import org.apache.flink.api.common.state.ListStateDescriptor;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.api.common.state.ReadOnlyBroadcastState;
import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.co.KeyedBroadcastProcessFunction;
import org.apache.flink.util.Collector;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ThermalSpikeProcessFunction extends KeyedBroadcastProcessFunction<String, AssetEvent, ThresholdConfig, Alert> {

    public static final String DEFAULT_ALERT_TYPE = "THERMAL_SPIKE";

    public static final MapStateDescriptor<String, Double> THRESHOLD_STATE_DESCRIPTOR =
            new MapStateDescriptor<>("thresholds-state", Types.STRING, Types.DOUBLE);

    private final double defaultThreshold;
    private final int windowSize;

    private transient ListState<Double> recentTemperaturesState;
    private transient ValueState<Boolean> alertActiveState;
    private transient ValueState<String> operatingModeState;

    public ThermalSpikeProcessFunction(double defaultThreshold) {
        this(defaultThreshold, 5);
    }

    public ThermalSpikeProcessFunction(double defaultThreshold, int windowSize) {
        this.defaultThreshold = defaultThreshold;
        this.windowSize = windowSize;
    }

    @Override
    public void open(Configuration parameters) throws Exception {
        ListStateDescriptor<Double> listDescriptor =
                new ListStateDescriptor<>("recent-temperatures-state", Types.DOUBLE);
        recentTemperaturesState = getRuntimeContext().getListState(listDescriptor);

        ValueStateDescriptor<Boolean> alertActiveDescriptor =
                new ValueStateDescriptor<>("alert-active-state", Types.BOOLEAN);
        alertActiveState = getRuntimeContext().getState(alertActiveDescriptor);

        ValueStateDescriptor<String> operatingModeDescriptor =
                new ValueStateDescriptor<>("operating-mode-state", Types.STRING);
        operatingModeState = getRuntimeContext().getState(operatingModeDescriptor);
    }

    @Override
    public void processElement(AssetEvent event, ReadOnlyContext ctx, Collector<Alert> out) throws Exception {
        if (event == null) {
            return;
        }

        if (event.getType() == AssetEvent.EventType.OPERATING_MODE) {
            AssetOperatingModeEvent modeEvent = event.getOperatingModeEvent();
            if (modeEvent != null && modeEvent.getOperatingMode() != null) {
                operatingModeState.update(modeEvent.getOperatingMode());
                if (OperatingMode.isMaintenanceMode(modeEvent.getOperatingMode())) {
                    alertActiveState.clear();
                }
            }
            return;
        }

        if (event.getType() == AssetEvent.EventType.TELEMETRY) {
            Telemetry value = event.getTelemetry();
            if (value == null) {
                return;
            }

            if (value.getSensorId() != null && isNonGeneratorOrVibrationSensor(value.getSensorId())) {
                return;
            }

            List<Double> recentTemperatures = new ArrayList<>();
            Iterable<Double> currentTemps = recentTemperaturesState.get();
            if (currentTemps != null) {
                for (Double temp : currentTemps) {
                    recentTemperatures.add(temp);
                }
            }

            recentTemperatures.add(value.getTemperature());
            while (recentTemperatures.size() > windowSize) {
                recentTemperatures.remove(0);
            }
            recentTemperaturesState.update(recentTemperatures);

            String currentMode = operatingModeState.value();
            if (OperatingMode.isMaintenanceMode(currentMode)) {
                return;
            }

            double sum = 0.0;
            for (double t : recentTemperatures) {
                sum += t;
            }
            double rollingAvg = recentTemperatures.isEmpty() ? 0.0 : sum / recentTemperatures.size();

            double effectiveThreshold = defaultThreshold;
            ReadOnlyBroadcastState<String, Double> broadcastState = ctx.getBroadcastState(THRESHOLD_STATE_DESCRIPTOR);
            if (broadcastState != null) {
                if (value.getAssetId() != null && broadcastState.contains(value.getAssetId())) {
                    effectiveThreshold = broadcastState.get(value.getAssetId());
                } else if (broadcastState.contains("GLOBAL")) {
                    effectiveThreshold = broadcastState.get("GLOBAL");
                }
            }

            boolean isAlertActive = Boolean.TRUE.equals(alertActiveState.value());

            if (rollingAvg > effectiveThreshold) {
                if (!isAlertActive) {
                    long epochMs = value.getTimestamp() != null ? value.getTimestamp().toEpochMilli() : System.currentTimeMillis();
                    String alertId = generateAlertId(value.getAssetId(), epochMs, DEFAULT_ALERT_TYPE);

                    Alert alert = new Alert(
                            alertId,
                            value.getAssetId(),
                            value.getSensorId(),
                            DEFAULT_ALERT_TYPE,
                            rollingAvg,
                            effectiveThreshold,
                            value.getTimestamp(),
                            String.format("Thermal spike detected on asset %s: rolling avg %.2f°C breaches threshold %.2f°C",
                                    value.getAssetId(), rollingAvg, effectiveThreshold)
                    );
                    out.collect(alert);
                    alertActiveState.update(true);
                }
            } else {
                if (isAlertActive) {
                    alertActiveState.clear();
                }
            }
        }
    }

    @Override
    public void processBroadcastElement(ThresholdConfig config, Context ctx, Collector<Alert> out) throws Exception {
        if (config == null) {
            return;
        }

        // Filter out threshold configs for other alert types
        if (config.getAlertType() != null && !config.getAlertType().trim().isEmpty()
                && !DEFAULT_ALERT_TYPE.equalsIgnoreCase(config.getAlertType().trim())) {
            return;
        }

        String key = getThresholdKey(config.getAssetId());
        ctx.getBroadcastState(THRESHOLD_STATE_DESCRIPTOR).put(key, config.getThreshold());
    }

    public static String getThresholdKey(String assetId) {
        if (assetId == null || assetId.trim().isEmpty() || assetId.equalsIgnoreCase("GLOBAL") || assetId.equalsIgnoreCase("DEFAULT")) {
            return "GLOBAL";
        }
        return assetId;
    }

    private boolean isNonGeneratorOrVibrationSensor(String sensorId) {
        String lower = sensorId.toLowerCase();
        return lower.contains("vibration") || lower.contains("vibr");
    }

    public static String generateAlertId(String assetId, long timestampMs, String alertType) {
        String rawKey = assetId + ":" + timestampMs + ":" + alertType;
        return UUID.nameUUIDFromBytes(rawKey.getBytes(StandardCharsets.UTF_8)).toString();
    }
}
