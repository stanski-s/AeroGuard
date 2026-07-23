package org.aeroguard.gateway;

import org.aeroguard.gateway.kafka.AlertKafkaConsumer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.kafka.listener.auto-startup=false"
})
class WebSocketGatewayTest {

    @LocalServerPort
    private int port;

    @Autowired
    private AlertKafkaConsumer alertKafkaConsumer;

    @Test
    void testWebSocketBroadcastsAlertToConnectedClient() throws Exception {
        BlockingQueue<String> receivedMessages = new ArrayBlockingQueue<>(10);

        StandardWebSocketClient client = new StandardWebSocketClient();
        WebSocketSession session = client.execute(new TextWebSocketHandler() {
            @Override
            protected void handleTextMessage(WebSocketSession session, TextMessage message) {
                receivedMessages.add(message.getPayload());
            }
        }, "ws://localhost:" + port + "/ws/alerts").get(5, TimeUnit.SECONDS);

        assertNotNull(session);
        assertTrue(session.isOpen());

        String sampleAlertJson = """
                {
                  "alert_id": "12345-67890",
                  "asset_id": "turbine-99",
                  "sensor_id": "temp-sensor-1",
                  "alert_type": "THERMAL_SPIKE",
                  "temperature": 92.5,
                  "threshold": 80.0,
                  "timestamp": "2026-07-23T17:00:00Z",
                  "message": "Thermal spike detected on asset turbine-99"
                }
                """;

        alertKafkaConsumer.consumeAlert(sampleAlertJson);

        String received = receivedMessages.poll(5, TimeUnit.SECONDS);
        assertNotNull(received, "WebSocket client should receive broadcasted alert");
        assertEquals(sampleAlertJson, received);

        session.close();
    }
}
