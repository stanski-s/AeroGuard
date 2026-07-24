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

public class TelemetrySimulator {
    private static final Logger logger = LoggerFactory.getLogger(TelemetrySimulator.class);
    private static final String TOPIC = "telemetry.raw";
    private static final String BOOTSTRAP_SERVERS = System.getenv().getOrDefault("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092");
    private static final int MESSAGES_PER_SECOND = Integer.parseInt(System.getenv().getOrDefault("RATE", "5"));

    private static final String[] ASSET_IDS = {
            "turbine-1", "turbine-2", "turbine-3", "turbine-4", "turbine-5", "turbine-6", "turbine-7",
            "microgrid-1", "microgrid-2", "substation-1"
    };

    public static void main(String[] args) {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());

        Map<String, Double> currentTemps = new HashMap<>();
        Random random = new Random();
        for (String id : ASSET_IDS) {
            currentTemps.put(id, 66.0 + random.nextDouble() * 4.0);
        }

        try (KafkaProducer<String, String> producer = new KafkaProducer<>(props)) {
            logger.info("Starting AeroGuard telemetry simulator. Sending {} messages per second to {}", MESSAGES_PER_SECOND, TOPIC);
            long sleepTimeMs = Math.max(100, 1000 / MESSAGES_PER_SECOND);
            int stepCounter = 0;

            while (!Thread.currentThread().isInterrupted()) {
                stepCounter++;
                for (String assetId : ASSET_IDS) {
                    double curTemp = currentTemps.get(assetId);
                    curTemp += (random.nextDouble() - 0.49) * 0.8;
                    curTemp = Math.max(58.0, Math.min(76.0, curTemp));
                    currentTemps.put(assetId, curTemp);

                    String sensorId = "temp-" + assetId;
                    double vibration = 0.12 + (random.nextDouble() * 0.22);
                    double formattedTemp = Math.round(curTemp * 100.0) / 100.0;
                    double formattedVib = Math.round(vibration * 1000.0) / 1000.0;

                    Telemetry telemetry = new Telemetry(assetId, sensorId, Instant.now(), formattedVib, formattedTemp);

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
