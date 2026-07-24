package org.aeroguard.gateway.kafka;

import org.aeroguard.gateway.handler.AlertWebSocketHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class AlertKafkaConsumer {

    private static final Logger logger = LoggerFactory.getLogger(AlertKafkaConsumer.class);
    private final AlertWebSocketHandler webSocketHandler;

    public AlertKafkaConsumer(AlertWebSocketHandler webSocketHandler) {
        this.webSocketHandler = webSocketHandler;
    }

    @KafkaListener(topics = "${app.kafka.topic.alerts:alerts.critical}", groupId = "${spring.kafka.consumer.group-id:websocket-gateway-group}")
    public void consumeAlert(String alertJson) {
        logger.info("Received critical alert from Kafka: {}", alertJson);
        webSocketHandler.broadcastTyped("ALERT", alertJson);
    }

    @KafkaListener(topics = "${app.kafka.topic.telemetry:telemetry.raw}", groupId = "websocket-gateway-telemetry-group")
    public void consumeTelemetry(String telemetryJson) {
        logger.debug("Received telemetry from Kafka: {}", telemetryJson);
        webSocketHandler.broadcastTyped("TELEMETRY", telemetryJson);
    }

    @KafkaListener(topics = "${app.kafka.topic.operating-mode:events.status}", groupId = "websocket-gateway-mode-group")
    public void consumeOperatingMode(String modeJson) {
        logger.info("Received operating mode update from Kafka: {}", modeJson);
        webSocketHandler.broadcastTyped("OPERATING_MODE", modeJson);
    }
}
