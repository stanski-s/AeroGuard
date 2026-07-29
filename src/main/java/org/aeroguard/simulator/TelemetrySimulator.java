package org.aeroguard.simulator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.aeroguard.model.Telemetry;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TelemetrySimulator {
    private static final Logger logger = LoggerFactory.getLogger(TelemetrySimulator.class);
    private static final String TOPIC = "telemetry.raw";
    private static final String BOOTSTRAP_SERVERS = System.getenv().getOrDefault("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092");
    private static final int MESSAGES_PER_SECOND = Integer.parseInt(System.getenv().getOrDefault("RATE", "5"));

    public enum FaultType {
        NONE,
        THERMAL_SPIKE,
        HIGH_VIBRATION,
        PITCH_ASYMMETRY,
        OVERSPEED
    }

    public static final String[] ASSET_IDS = {
            "BAL-WTG-001", "BAL-WTG-002", "BAL-WTG-003", "BAL-WTG-004", "BAL-WTG-005",
            "BAL-WTG-006", "BAL-WTG-007", "BAL-WTG-008", "BAL-WTG-009", "BAL-WTG-010"
    };

    private final Map<String, TurbineState> states = new ConcurrentHashMap<>();
    private final Random random;

    public TelemetrySimulator() {
        this(new Random());
    }

    public TelemetrySimulator(Random random) {
        this.random = random;
        for (String id : ASSET_IDS) {
            states.put(id, new TurbineState(id, random));
        }
    }

    public static class TurbineState {
        private final String assetId;
        private double rotorSpeedRpm;
        private double powerOutputMw;
        private double pitchAngleDeg;
        private double temperature;
        private double nacelleTempC;
        private double vibration;
        private FaultType activeFault = FaultType.NONE;

        public TurbineState(String assetId, Random random) {
            this.assetId = assetId;
            this.rotorSpeedRpm = 7.2 + (random.nextDouble() - 0.5) * 0.4;
            this.powerOutputMw = 12.5 + (random.nextDouble() - 0.5) * 0.5;
            this.pitchAngleDeg = 4.2 + (random.nextDouble() - 0.5) * 0.2;
            this.temperature = 66.0 + (random.nextDouble() - 0.5) * 4.0;
            this.nacelleTempC = 38.0 + (random.nextDouble() - 0.5) * 2.0;
            this.vibration = 0.20 + (random.nextDouble() - 0.5) * 0.05;
        }

        public synchronized Telemetry step(Random random) {
            switch (activeFault) {
                case THERMAL_SPIKE:
                    temperature = Math.min(96.0, temperature + 1.8 + random.nextDouble() * 0.8);
                    nacelleTempC = Math.min(52.0, nacelleTempC + 0.6 + random.nextDouble() * 0.3);
                    vibration = 0.22 + random.nextDouble() * 0.15;
                    rotorSpeedRpm = Math.max(6.0, rotorSpeedRpm - 0.05);
                    powerOutputMw = Math.max(10.0, powerOutputMw - 0.1);
                    break;

                case HIGH_VIBRATION:
                    vibration = 3.2 + random.nextDouble() * 2.8;
                    temperature += (random.nextDouble() - 0.4) * 0.5;
                    break;

                case OVERSPEED:
                    rotorSpeedRpm = Math.min(9.4, rotorSpeedRpm + 0.25 + random.nextDouble() * 0.1);
                    powerOutputMw = Math.min(15.8, 12.0 + (rotorSpeedRpm - 6.8) * 1.6);
                    pitchAngleDeg = 4.2 + (random.nextDouble() - 0.5) * 0.4;
                    vibration = 0.35 + random.nextDouble() * 0.25;
                    break;

                case PITCH_ASYMMETRY:
                    pitchAngleDeg = 6.8 + (random.nextDouble() - 0.5) * 0.6;
                    vibration = 0.45 + random.nextDouble() * 0.30;
                    powerOutputMw = Math.max(9.5, powerOutputMw - 0.2);
                    break;

                case NONE:
                default:
                    // Physics correlation in normal operating mode
                    rotorSpeedRpm = Math.max(6.8, Math.min(8.2, rotorSpeedRpm + (random.nextDouble() - 0.5) * 0.15));
                    
                    double targetPower = 11.8 + (rotorSpeedRpm - 6.8) * (2.4 / 1.4);
                    powerOutputMw = Math.max(11.8, Math.min(14.2, targetPower + (random.nextDouble() - 0.5) * 0.2));

                    double targetPitch = 3.8 + (rotorSpeedRpm - 6.8) * (1.2 / 1.4);
                    pitchAngleDeg = Math.max(3.8, Math.min(5.0, targetPitch + (random.nextDouble() - 0.5) * 0.1));

                    double targetTemp = 64.0 + (powerOutputMw - 11.8) * 3.5;
                    temperature += (targetTemp - temperature) * 0.1 + (random.nextDouble() - 0.49) * 0.3;
                    temperature = Math.max(58.0, Math.min(76.0, temperature));

                    double targetNacelle = 34.0 + (temperature - 58.0) * (8.0 / 18.0);
                    nacelleTempC += (targetNacelle - nacelleTempC) * 0.1 + (random.nextDouble() - 0.49) * 0.2;
                    nacelleTempC = Math.max(34.0, Math.min(42.0, nacelleTempC));

                    vibration = 0.12 + (random.nextDouble() * 0.22);
                    break;
            }

            double formattedTemp = Math.round(temperature * 100.0) / 100.0;
            double formattedVib = Math.round(vibration * 1000.0) / 1000.0;
            double formattedPower = Math.round(powerOutputMw * 10.0) / 10.0;
            double formattedPitch = Math.round(pitchAngleDeg * 10.0) / 10.0;
            double formattedRpm = Math.round(rotorSpeedRpm * 10.0) / 10.0;
            double formattedNacelle = Math.round(nacelleTempC * 10.0) / 10.0;

            return new Telemetry(
                    assetId, Instant.now(), formattedVib, formattedTemp,
                    formattedPower, formattedPitch, formattedRpm, formattedNacelle
            );
        }

        public synchronized void setFault(FaultType fault) {
            this.activeFault = fault;
        }

        public synchronized FaultType getFault() {
            return this.activeFault;
        }
    }

    public Telemetry generateNextTelemetry(String assetId) {
        TurbineState state = states.computeIfAbsent(assetId, id -> new TurbineState(id, random));
        return state.step(random);
    }

    public void injectFault(String assetId, FaultType fault) {
        TurbineState state = states.get(assetId);
        if (state != null) {
            state.setFault(fault);
            logger.info("Injected fault {} on asset {}", fault, assetId);
        }
    }

    public void clearFault(String assetId) {
        injectFault(assetId, FaultType.NONE);
    }

    public static Telemetry generateTelemetry(String assetId, double curTemp, Random random) {
        double vibration = 0.12 + (random.nextDouble() * 0.22);
        double formattedTemp = Math.round(curTemp * 100.0) / 100.0;
        double formattedVib = Math.round(vibration * 1000.0) / 1000.0;

        double powerOutputMw = Math.round((11.8 + random.nextDouble() * 2.4) * 10.0) / 10.0;
        double pitchAngleDeg = Math.round((3.8 + random.nextDouble() * 1.2) * 10.0) / 10.0;
        double rotorSpeedRpm = Math.round((6.8 + random.nextDouble() * 1.4) * 10.0) / 10.0;
        double nacelleTempC = Math.round((34.0 + random.nextDouble() * 8.0) * 10.0) / 10.0;

        return new Telemetry(
                assetId, Instant.now(), formattedVib, formattedTemp,
                powerOutputMw, pitchAngleDeg, rotorSpeedRpm, nacelleTempC
        );
    }

    public static void main(String[] args) {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());

        TelemetrySimulator simulator = new TelemetrySimulator();

        try (KafkaProducer<String, String> producer = new KafkaProducer<>(props)) {
            logger.info("Starting AeroGuard telemetry simulator. Sending {} messages per second to {}", MESSAGES_PER_SECOND, TOPIC);
            long sleepTimeMs = Math.max(100, 1000 / MESSAGES_PER_SECOND);
            int stepCounter = 0;

            while (!Thread.currentThread().isInterrupted()) {
                stepCounter++;

                // Periodically inject realistic faults for live demonstration / alerting testing
                if (stepCounter == 100) {
                    simulator.injectFault("BAL-WTG-003", FaultType.THERMAL_SPIKE);
                } else if (stepCounter == 250) {
                    simulator.injectFault("BAL-WTG-006", FaultType.HIGH_VIBRATION);
                } else if (stepCounter == 400) {
                    simulator.injectFault("BAL-WTG-004", FaultType.OVERSPEED);
                } else if (stepCounter == 600) {
                    simulator.clearFault("BAL-WTG-003");
                    simulator.clearFault("BAL-WTG-006");
                    simulator.clearFault("BAL-WTG-004");
                }

                for (String assetId : ASSET_IDS) {
                    Telemetry telemetry = simulator.generateNextTelemetry(assetId);

                    try {
                        String value = mapper.writeValueAsString(telemetry);
                        ProducerRecord<String, String> record = new ProducerRecord<>(TOPIC, assetId, value);
                        producer.send(record);
                    } catch (Exception e) {
                        logger.error("Error serializing telemetry for {}", assetId, e);
                    }
                }

                try {
                    Thread.sleep(sleepTimeMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }
}
