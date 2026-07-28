package org.aeroguard.simulator;

import org.aeroguard.model.Telemetry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TelemetrySimulatorTest {

    private TelemetrySimulator simulator;

    @BeforeEach
    void setUp() {
        simulator = new TelemetrySimulator();
    }

    @Test
    void testNormalPhysicsCorrelation() {
        // Run simulator for 20 steps under normal operation
        Telemetry prev = null;
        for (int i = 0; i < 20; i++) {
            Telemetry current = simulator.generateNextTelemetry("turbine-1");
            assertNotNull(current);

            // Bounds check for normal operation as specified in 01-extended-telemetry-model.md
            assertTrue(current.getPowerOutputMw() >= 11.8 && current.getPowerOutputMw() <= 14.2,
                    "Power output should be within spec bounds (11.8 - 14.2 MW): " + current.getPowerOutputMw());
            assertTrue(current.getPitchAngleDeg() >= 3.8 && current.getPitchAngleDeg() <= 5.0,
                    "Pitch angle should be within spec bounds (3.8 - 5.0°): " + current.getPitchAngleDeg());
            assertTrue(current.getRotorSpeedRpm() >= 6.8 && current.getRotorSpeedRpm() <= 8.2,
                    "Rotor speed should be within spec bounds (6.8 - 8.2 RPM): " + current.getRotorSpeedRpm());
            assertTrue(current.getNacelleTempC() >= 34.0 && current.getNacelleTempC() <= 42.0,
                    "Nacelle temp should be within spec bounds (34.0 - 42.0°C): " + current.getNacelleTempC());
            assertTrue(current.getVibration() >= 0.05 && current.getVibration() <= 1.0,
                    "Vibration should be within normal bounds: " + current.getVibration());

            prev = current;
        }
    }

    @Test
    void testThermalSpikeFaultInjection() {
        String assetId = "turbine-3";
        simulator.injectFault(assetId, TelemetrySimulator.FaultType.THERMAL_SPIKE);

        // Advance simulation steps
        Telemetry telemetry = null;
        for (int i = 0; i < 15; i++) {
            telemetry = simulator.generateNextTelemetry(assetId);
        }

        assertNotNull(telemetry);
        assertTrue(telemetry.getTemperature() > 75.0,
                "Temperature should increase above warning threshold (75°C) during thermal spike, got: " + telemetry.getTemperature());
        assertTrue(telemetry.getNacelleTempC() > 40.0,
                "Nacelle temp should elevate during thermal spike, got: " + telemetry.getNacelleTempC());
    }

    @Test
    void testHighVibrationFaultInjection() {
        String assetId = "turbine-2";
        simulator.injectFault(assetId, TelemetrySimulator.FaultType.HIGH_VIBRATION);

        Telemetry telemetry = null;
        for (int i = 0; i < 10; i++) {
            telemetry = simulator.generateNextTelemetry(assetId);
        }

        assertNotNull(telemetry);
        assertTrue(telemetry.getVibration() > 2.5,
                "Vibration should exceed warning threshold (2.5 mm/s) during high vibration fault, got: " + telemetry.getVibration());
    }

    @Test
    void testOverspeedFaultInjection() {
        String assetId = "turbine-4";
        simulator.injectFault(assetId, TelemetrySimulator.FaultType.OVERSPEED);

        Telemetry telemetry = null;
        for (int i = 0; i < 10; i++) {
            telemetry = simulator.generateNextTelemetry(assetId);
        }

        assertNotNull(telemetry);
        assertTrue(telemetry.getRotorSpeedRpm() > 8.0,
                "Rotor speed should exceed overspeed threshold (8.0 RPM), got: " + telemetry.getRotorSpeedRpm());
    }

    @Test
    void testPitchAsymmetryFaultInjection() {
        String assetId = "turbine-5";
        simulator.injectFault(assetId, TelemetrySimulator.FaultType.PITCH_ASYMMETRY);

        Telemetry telemetry = null;
        for (int i = 0; i < 10; i++) {
            telemetry = simulator.generateNextTelemetry(assetId);
        }

        assertNotNull(telemetry);
        assertTrue(telemetry.getPitchAngleDeg() > 6.0 || telemetry.getPitchAngleDeg() < 2.0,
                "Pitch angle should deviate abnormally during pitch fault, got: " + telemetry.getPitchAngleDeg());
    }

    @Test
    void testClearFaultRestoresNormalOperation() {
        String assetId = "turbine-2";
        simulator.injectFault(assetId, TelemetrySimulator.FaultType.HIGH_VIBRATION);
        simulator.generateNextTelemetry(assetId);

        simulator.clearFault(assetId);

        Telemetry telemetry = null;
        for (int i = 0; i < 20; i++) {
            telemetry = simulator.generateNextTelemetry(assetId);
        }

        assertNotNull(telemetry);
        assertTrue(telemetry.getVibration() < 1.0,
                "Vibration should return to normal levels after clearing fault, got: " + telemetry.getVibration());
    }
}
