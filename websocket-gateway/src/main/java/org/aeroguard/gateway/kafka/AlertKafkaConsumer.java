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
        webSocketHandler.broadcast(alertJson);
    }
}
