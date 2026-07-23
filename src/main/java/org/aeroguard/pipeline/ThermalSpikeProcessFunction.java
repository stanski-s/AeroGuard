package org.aeroguard.pipeline;

import org.aeroguard.model.Alert;
import org.aeroguard.model.Telemetry;
import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.util.Collector;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ThermalSpikeProcessFunction extends KeyedProcessFunction<String, Telemetry, Alert> {

    private final double defaultThreshold;
    private final int windowSize;
    private transient ValueState<RollingState> rollingState;

    public ThermalSpikeProcessFunction(double defaultThreshold) {
        this(defaultThreshold, 5);
    }

    public ThermalSpikeProcessFunction(double defaultThreshold, int windowSize) {
        this.defaultThreshold = defaultThreshold;
        this.windowSize = windowSize;
    }

    @Override
    public void open(Configuration parameters) throws Exception {
        ValueStateDescriptor<RollingState> descriptor =
                new ValueStateDescriptor<>("rolling-temperature-state", RollingState.class);
        rollingState = getRuntimeContext().getState(descriptor);
    }

    @Override
    public void processElement(Telemetry value, Context ctx, Collector<Alert> out) throws Exception {
        // Filter out non-generator or non-temperature telemetry if sensor ID is specified
        if (value.getSensorId() != null && isNonGeneratorOrVibrationSensor(value.getSensorId())) {
            return;
        }

        RollingState state = rollingState.value();
        if (state == null) {
            state = new RollingState(windowSize);
        }

        state.addTemperature(value.getTemperature());

        double rollingAvg = state.getAverage();
        if (rollingAvg > defaultThreshold) {
            if (!state.isAlertActive()) {
                long epochMs = value.getTimestamp() != null ? value.getTimestamp().toEpochMilli() : System.currentTimeMillis();
                String alertId = generateAlertId(value.getAssetId(), epochMs, "THERMAL_SPIKE");
                
                Alert alert = new Alert(
                        alertId,
                        value.getAssetId(),
                        value.getSensorId(),
                        "THERMAL_SPIKE",
                        rollingAvg,
                        defaultThreshold,
                        value.getTimestamp(),
                        String.format("Thermal spike detected on asset %s: rolling avg %.2f°C breaches threshold %.2f°C",
                                value.getAssetId(), rollingAvg, defaultThreshold)
                );
                out.collect(alert);
                state.setAlertActive(true);
            }
        } else {
            if (state.isAlertActive()) {
                state.setAlertActive(false);
            }
        }
        rollingState.update(state);
    }

    private boolean isNonGeneratorOrVibrationSensor(String sensorId) {
        String lower = sensorId.toLowerCase();
        return lower.contains("vibration") || lower.contains("vibr");
    }

    public static String generateAlertId(String assetId, long timestampMs, String alertType) {
        String rawKey = assetId + ":" + timestampMs + ":" + alertType;
        return UUID.nameUUIDFromBytes(rawKey.getBytes(StandardCharsets.UTF_8)).toString();
    }

    public static class RollingState implements Serializable {
        private List<Double> recentTemperatures = new ArrayList<>();
        private int windowSize = 5;
        private boolean alertActive = false;

        public RollingState() {}

        public RollingState(int windowSize) {
            this.windowSize = windowSize;
        }

        public void addTemperature(double temp) {
            recentTemperatures.add(temp);
            if (recentTemperatures.size() > windowSize) {
                recentTemperatures.remove(0);
            }
        }

        public double getAverage() {
            if (recentTemperatures.isEmpty()) return 0.0;
            double sum = 0;
            for (double t : recentTemperatures) {
                sum += t;
            }
            return sum / recentTemperatures.size();
        }

        public boolean isAlertActive() {
            return alertActive;
        }

        public void setAlertActive(boolean alertActive) {
            this.alertActive = alertActive;
        }

        public int getWindowSize() {
            return windowSize;
        }

        public void setWindowSize(int windowSize) {
            this.windowSize = windowSize;
        }

        public List<Double> getRecentTemperatures() {
            return recentTemperatures;
        }

        public void setRecentTemperatures(List<Double> recentTemperatures) {
            this.recentTemperatures = recentTemperatures;
        }
    }
}
