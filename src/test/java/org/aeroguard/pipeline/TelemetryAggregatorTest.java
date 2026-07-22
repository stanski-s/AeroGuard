package org.aeroguard.pipeline;

import org.aeroguard.model.Telemetry;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TelemetryAggregatorTest {

    @Test
    void testAggregatorCreatesCorrectResult() {
        TelemetryPipeline.TelemetryAggregator aggregator = new TelemetryPipeline.TelemetryAggregator();
        TelemetryPipeline.Accumulator acc = aggregator.createAccumulator();
        
        Instant t1 = Instant.parse("2024-01-01T10:01:00Z");
        Telemetry tm1 = new Telemetry("asset1", "sensor1", t1, 2.5, 60.0);
        acc = aggregator.add(tm1, acc);
        
        Instant t2 = Instant.parse("2024-01-01T10:02:00Z");
        Telemetry tm2 = new Telemetry("asset1", "sensor1", t2, 3.5, 75.0);
        acc = aggregator.add(tm2, acc);
        
        TelemetryPipeline.TurbineMetric result = aggregator.getResult(acc);
        
        assertEquals("asset1", result.assetId);
        assertEquals(3.0, result.avgVibration, 0.01);
        assertEquals(75.0, result.maxTemperature, 0.01);
        assertEquals(Instant.parse("2024-01-01T10:00:00Z"), result.time);
    }
}
