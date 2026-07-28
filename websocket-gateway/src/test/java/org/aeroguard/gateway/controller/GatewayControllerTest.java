package org.aeroguard.gateway.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GatewayControllerTest {

    private MockMvc mockMvc;
    private KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        kafkaTemplate = mock(KafkaTemplate.class);
        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(null));

        GatewayController controller = new GatewayController(kafkaTemplate);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void triggerDiagnosticAction_withValidAction_shouldPublishToKafkaAndReturnSuccess() throws Exception {
        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);

        mockMvc.perform(post("/api/assets/turbine-42/action")
                        .param("action", "LOCK_BRAKES"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.assetId").value("turbine-42"))
                .andExpect(jsonPath("$.action").value("LOCK_BRAKES"));

        verify(kafkaTemplate).send(eq("events.status"), eq("turbine-42"), payloadCaptor.capture());

        String capturedPayload = payloadCaptor.getValue();
        assertNotNull(capturedPayload);

        JsonNode jsonNode = objectMapper.readTree(capturedPayload);
        assertEquals("turbine-42", jsonNode.get("assetId").asText());
        assertEquals("LOCK_BRAKES", jsonNode.get("action").asText());
        assertEquals("DIAGNOSTIC_ACTION", jsonNode.get("eventType").asText());
        assertNotNull(jsonNode.get("timestamp"));
    }

    @Test
    void triggerDiagnosticAction_withAllAllowedActionsAndCaseInsensitive_shouldSucceed() throws Exception {
        String[] actions = {"DERATE_POWER", "recalibrate_pitch", "dispatch_tech"};
        String[] expectedNormalized = {"DERATE_POWER", "RECALIBRATE_PITCH", "DISPATCH_TECH"};

        for (int i = 0; i < actions.length; i++) {
            performActionAndAssertSuccess("turbine-7", actions[i], expectedNormalized[i]);
        }
    }

    @Test
    void triggerDiagnosticAction_withInvalidAction_shouldReturnBadRequest() throws Exception {
        mockMvc.perform(post("/api/assets/turbine-42/action")
                        .param("action", "INVALID_ACTION"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.message").exists());

        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    void triggerDiagnosticAction_whenKafkaFails_shouldReturnInternalServerError() throws Exception {
        CompletableFuture<org.springframework.kafka.support.SendResult<String, String>> failedFuture =
                new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("Kafka cluster unreachable"));

        when(kafkaTemplate.send(anyString(), anyString(), anyString())).thenReturn(failedFuture);

        mockMvc.perform(post("/api/assets/turbine-42/action")
                        .param("action", "LOCK_BRAKES"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.message").exists());
    }

    private void performActionAndAssertSuccess(String assetId, String rawAction, String expectedAction) throws Exception {
        mockMvc.perform(post("/api/assets/" + assetId + "/action")
                        .param("action", rawAction))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.assetId").value(assetId))
                .andExpect(jsonPath("$.action").value(expectedAction));
    }
}
