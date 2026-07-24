package org.aeroguard.gateway.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AlertWebSocketHandler extends TextWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(AlertWebSocketHandler.class);
    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.add(session);
        logger.info("WebSocket client connected: {}", session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session);
        logger.info("WebSocket client disconnected: {}", session.getId());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        logger.error("WebSocket transport error on session {}: {}", session.getId(), exception.getMessage());
        sessions.remove(session);
    }

    public void broadcast(String message) {
        broadcastMessage(message);
    }

    public void broadcastTyped(String type, String rawJsonPayload) {
        try {
            Object parsedPayload = objectMapper.readValue(rawJsonPayload, Object.class);
            Map<String, Object> frame = new HashMap<>();
            frame.put("type", type);
            frame.put("payload", parsedPayload);

            String formattedMessage = objectMapper.writeValueAsString(frame);
            broadcastMessage(formattedMessage);
        } catch (Exception e) {
            logger.error("Failed to construct typed frame for type {}: {}", type, e.getMessage());
            broadcastMessage(rawJsonPayload);
        }
    }

    private void broadcastMessage(String message) {
        if (sessions.isEmpty()) {
            return;
        }
        TextMessage textMessage = new TextMessage(message);
        for (WebSocketSession session : sessions) {
            if (session.isOpen()) {
                try {
                    session.sendMessage(textMessage);
                } catch (IOException e) {
                    logger.error("Failed to send message to session {}: {}", session.getId(), e.getMessage());
                }
            }
        }
    }
}
