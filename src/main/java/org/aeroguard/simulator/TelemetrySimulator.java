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
import java.util.Properties;
import java.util.Random;

public class TelemetrySimulator {
    private static final Logger logger = LoggerFactory.getLogger(TelemetrySimulator.class);
    private static final String TOPIC = "telemetry.raw";
    private static final String BOOTSTRAP_SERVERS = System.getenv().getOrDefault("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092");
    private static final int MESSAGES_PER_SECOND = Integer.parseInt(System.getenv().getOrDefault("RATE", "10"));

    public static void main(String[] args) {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());

        try (KafkaProducer<String, String> producer = new KafkaProducer<>(props)) {
            logger.info("Starting telemetry simulator. Sending {} messages per second to {}", MESSAGES_PER_SECOND, TOPIC);
            Random random = new Random();
            long sleepTimeMs = 1000 / MESSAGES_PER_SECOND;

            while (!Thread.currentThread().isInterrupted()) {
                String assetId = "turbine-" + (random.nextInt(10) + 1);
                String sensorId = "sensor-" + (random.nextInt(5) + 1);
                double vibration = 1.0 + (random.nextDouble() * 5.0); // 1.0 to 6.0
                double temperature = 50.0 + (random.nextDouble() * 50.0); // 50.0 to 100.0

                Telemetry telemetry = new Telemetry(assetId, sensorId, Instant.now(), vibration, temperature);

                try {
                    String value = mapper.writeValueAsString(telemetry);
                    ProducerRecord<String, String> record = new ProducerRecord<>(TOPIC, assetId, value);
                    producer.send(record, (metadata, exception) -> {
                        if (exception != null) {
                            logger.error("Error sending message", exception);
                        }
                    });
                    
                    Thread.sleep(sleepTimeMs);
                } catch (Exception e) {
                    logger.error("Error processing telemetry", e);
                    if (e instanceof InterruptedException) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }
    }
}
