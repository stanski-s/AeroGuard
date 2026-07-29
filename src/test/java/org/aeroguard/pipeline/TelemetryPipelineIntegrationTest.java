package org.aeroguard.pipeline;

import org.aeroguard.model.Alert;
import org.aeroguard.model.AssetEvent;
import org.aeroguard.model.AssetOperatingModeEvent;
import org.aeroguard.model.ConfigEvent;
import org.aeroguard.model.DiagnosticAction;
import org.aeroguard.model.DiagnosticActionRule;
import org.aeroguard.model.Telemetry;
import org.aeroguard.model.ThresholdConfig;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.streaming.api.datastream.BroadcastStream;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.sink.SinkFunction;
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TelemetryPipelineIntegrationTest {

    private static final List<Alert> collectedAlerts = Collections.synchronizedList(new ArrayList<>());
    private static final List<TelemetryPipeline.AssetMetric> collectedMetrics = Collections.synchronizedList(new ArrayList<>());

    @BeforeEach
    void setUp() {
        collectedAlerts.clear();
        collectedMetrics.clear();
    }

    public static class CollectAlertSink implements SinkFunction<Alert> {
        @Override
        public void invoke(Alert value, Context context) {
            collectedAlerts.add(value);
        }
    }

    public static class CollectMetricSink implements SinkFunction<TelemetryPipeline.AssetMetric> {
        @Override
        public void invoke(TelemetryPipeline.AssetMetric value, Context context) {
            collectedMetrics.add(value);
        }
    }

    @Test
    void testEndToEndTelemetryPipelineExecution() throws Exception {
        org.apache.flink.configuration.Configuration flinkConfig = new org.apache.flink.configuration.Configuration();
        flinkConfig.setString("security.delegation.token.provider.hadoopfs.enabled", "false");
        flinkConfig.setString("security.delegation.token.provider.hbase.enabled", "false");

        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment(flinkConfig);
        env.setParallelism(1);

        Instant baseTime = Instant.parse("2026-07-29T12:00:00Z");

        // 1. Prepare Telemetry & Operating Mode Events
        Telemetry t1 = new Telemetry("INT-WTG-01", baseTime, 0.4, 65.0);
        t1.setPowerOutputMw(12.5);
        t1.setPitchAngleDeg(4.2);
        t1.setRotorSpeedRpm(7.5);
        t1.setNacelleTempC(36.0);

        Telemetry t2 = new Telemetry("INT-WTG-01", baseTime.plusSeconds(10), 0.5, 88.0); // Breach threshold 80.0
        t2.setPowerOutputMw(13.0);
        t2.setPitchAngleDeg(4.1);
        t2.setRotorSpeedRpm(7.8);
        t2.setNacelleTempC(41.0);

        Telemetry t3Maintenance = new Telemetry("INT-WTG-02", baseTime.plusSeconds(15), 0.6, 92.0); // High temp but maintenance mode

        AssetOperatingModeEvent modeMaint = new AssetOperatingModeEvent("INT-WTG-02", "MAINTENANCE_MODE", baseTime);
        AssetOperatingModeEvent modeOnline = new AssetOperatingModeEvent("INT-WTG-01", "ONLINE", baseTime);

        DataStream<Telemetry> telemetryStream = env.fromElements(t1, t2, t3Maintenance)
                .assignTimestampsAndWatermarks(
                        WatermarkStrategy.<Telemetry>forBoundedOutOfOrderness(Duration.ofSeconds(1))
                                .withTimestampAssigner((event, timestamp) -> event.getTimestamp().toEpochMilli())
                );

        // 2. Prepare Broadcast Configuration Stream
        ThresholdConfig customThreshold = new ThresholdConfig("INT-WTG-01", 75.0, "THERMAL_SPIKE");
        DiagnosticAction customAction = new DiagnosticAction(
                "ACT-INT-01",
                "Remote Pitch Adjustment",
                "Adjust pitch blades by 10 degrees to cool generator",
                "HIGH",
                80,
                "AUTOMATED_SYSTEM"
        );
        DiagnosticActionRule customRule = new DiagnosticActionRule("RULE-INT-01", "INT-WTG-01", "THERMAL_SPIKE", "ONLINE", customAction);

        DataStream<ConfigEvent> configStream = env.fromElements(
                ConfigEvent.fromThreshold(customThreshold),
                ConfigEvent.fromDiagnosticActionRule(customRule)
        );

        BroadcastStream<ConfigEvent> broadcastConfig = configStream.broadcast(
                ThermalSpikeProcessFunction.THRESHOLD_STATE_DESCRIPTOR,
                ThermalSpikeProcessFunction.DIAGNOSTIC_ACTION_STATE_DESCRIPTOR
        );

        // 3. Window Aggregation Topology
        DataStream<TelemetryPipeline.AssetMetric> aggregatedStream = telemetryStream
                .keyBy(Telemetry::getAssetId)
                .window(TumblingEventTimeWindows.of(Time.minutes(1)))
                .aggregate(new TelemetryPipeline.TelemetryAggregator());

        aggregatedStream.addSink(new CollectMetricSink());

        // 4. Keyed Broadcast Alerting Stream Topology
        DataStream<AssetEvent> combinedAssetStream = env.fromElements(
                AssetEvent.fromOperatingMode(modeMaint),
                AssetEvent.fromOperatingMode(modeOnline),
                AssetEvent.fromTelemetry(t1),
                AssetEvent.fromTelemetry(t2),
                AssetEvent.fromTelemetry(t3Maintenance)
        ).assignTimestampsAndWatermarks(
                WatermarkStrategy.<AssetEvent>forBoundedOutOfOrderness(Duration.ofSeconds(1))
                        .withIdleness(Duration.ofSeconds(1))
                        .withTimestampAssigner((event, timestamp) -> {
                            if (event.getTelemetry() != null && event.getTelemetry().getTimestamp() != null) {
                                return event.getTelemetry().getTimestamp().toEpochMilli();
                            } else if (event.getOperatingModeEvent() != null && event.getOperatingModeEvent().getTimestamp() != null) {
                                return event.getOperatingModeEvent().getTimestamp().toEpochMilli();
                            }
                            return System.currentTimeMillis();
                        })
        );

        DataStream<Alert> alertStream = combinedAssetStream
                .keyBy(AssetEvent::getAssetId)
                .connect(broadcastConfig)
                .process(new ThermalSpikeProcessFunction(80.0, 5));

        alertStream.addSink(new CollectAlertSink());

        // Execute pipeline job
        env.execute("Telemetry Pipeline Integration Test");

        // Verify Aggregation Metrics
        assertFalse(collectedMetrics.isEmpty(), "Metrics aggregation stream should contain window metrics");
        TelemetryPipeline.AssetMetric metric1 = collectedMetrics.stream()
                .filter(m -> "INT-WTG-01".equals(m.assetId))
                .findFirst()
                .orElse(null);

        assertNotNull(metric1);
        assertEquals(88.0, metric1.maxTemperature, 0.001);
        assertEquals(0.45, metric1.avgVibration, 0.001);

        // Verify Alerts Emitted & Suppressed
        assertFalse(collectedAlerts.isEmpty(), "Alert stream should emit critical alerts");
        assertEquals(1, collectedAlerts.size(), "Only INT-WTG-01 should emit alert; INT-WTG-02 is in MAINTENANCE_MODE");

        Alert alert = collectedAlerts.get(0);
        assertEquals("INT-WTG-01", alert.getAssetId());
        assertEquals("THERMAL_SPIKE", alert.getAlertType());
        assertEquals(76.5, alert.getTemperature(), 0.001, "Alert temperature should be the rolling average (65.0 + 88.0)/2 = 76.5°C");
        assertEquals(75.0, alert.getThreshold(), 0.001, "Should use broadcasted dynamic threshold 75.0 instead of default 80.0");
        assertNotNull(alert.getDiagnosticAction());
        assertEquals("ACT-INT-01", alert.getDiagnosticAction().getActionId());
        assertEquals("Remote Pitch Adjustment", alert.getDiagnosticAction().getTitle());
    }
}
