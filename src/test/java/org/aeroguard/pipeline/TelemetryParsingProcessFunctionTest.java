package org.aeroguard.pipeline;

import org.aeroguard.model.Telemetry;
import org.apache.flink.streaming.api.operators.ProcessOperator;
import org.apache.flink.streaming.runtime.streamrecord.StreamRecord;
import org.apache.flink.streaming.util.OneInputStreamOperatorTestHarness;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TelemetryParsingProcessFunctionTest {

    private OneInputStreamOperatorTestHarness<String, Telemetry> harness;

    @BeforeEach
    void setUp() throws Exception {
        TelemetryPipeline.TelemetryParsingProcessFunction fn = new TelemetryPipeline.TelemetryParsingProcessFunction();
        ProcessOperator<String, Telemetry> operator = new ProcessOperator<>(fn);

        harness = new OneInputStreamOperatorTestHarness<>(operator);
        harness.open();
    }

    @AfterEach
    void tearDown() throws Exception {
        if (harness != null) {
            harness.close();
        }
    }

    @Test
    void testValidJsonEmitsTelemetryToMainOutput() throws Exception {
        String json = "{"
                + "\"asset_id\":\"turbine-101\","
                + "\"timestamp\":\"2026-07-29T10:00:00Z\","
                + "\"vibration\":0.45,"
                + "\"temperature\":82.5,"
                + "\"power_output_mw\":12.4,"
                + "\"pitch_angle_deg\":4.2,"
                + "\"rotor_speed_rpm\":7.5,"
                + "\"nacelle_temp_c\":38.1"
                + "}";

        harness.processElement(new StreamRecord<>(json));

        List<Object> output = new java.util.ArrayList<>(harness.getOutput());
        assertEquals(1, output.size());
        Telemetry telemetry = (Telemetry) ((StreamRecord<?>) output.getFirst()).getValue();
        assertEquals("turbine-101", telemetry.getAssetId());
        assertEquals(82.5, telemetry.getTemperature(), 0.001);
    }

    @Test
    void testMalformedJsonRoutesToDlqSideOutput() throws Exception {
        String malformedJson = "{ invalid json content }";

        harness.processElement(new StreamRecord<>(malformedJson));

        // Main output should be empty
        List<Object> output = new java.util.ArrayList<>(harness.getOutput());
        assertTrue(output.isEmpty(), "Malformed JSON should not produce main output record");

        // DLQ Side output should contain the raw malformed string
        java.util.Queue<StreamRecord<String>> dlqOutput = harness.getSideOutput(TelemetryPipeline.DLQ_TAG);
        assertNotNull(dlqOutput);
        assertEquals(1, dlqOutput.size());
        assertEquals(malformedJson, dlqOutput.peek().getValue());
    }
}
