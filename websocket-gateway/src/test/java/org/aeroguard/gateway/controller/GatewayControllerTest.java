package org.aeroguard.gateway.controller;

import org.aeroguard.gateway.exception.EventPublishingException;
import org.aeroguard.gateway.publisher.ControlEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GatewayControllerTest {

    private MockMvc mockMvc;
    private ControlEventPublisher eventPublisher;

    @BeforeEach
    void setUp() {
        eventPublisher = mock(ControlEventPublisher.class);
        GatewayController controller = new GatewayController(eventPublisher);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void triggerDiagnosticAction_withValidAction_shouldPublishToSeamAndReturnSuccess() throws Exception {
        mockMvc.perform(post("/api/assets/turbine-42/action")
                        .param("action", "LOCK_BRAKES"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.assetId").value("turbine-42"))
                .andExpect(jsonPath("$.action").value("LOCK_BRAKES"));

        verify(eventPublisher).publishDiagnosticAction("turbine-42", "LOCK_BRAKES");
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

        verifyNoInteractions(eventPublisher);
    }

    @Test
    void triggerDiagnosticAction_whenPublisherFails_shouldReturnInternalServerError() throws Exception {
        doThrow(new EventPublishingException("Kafka cluster unreachable"))
                .when(eventPublisher).publishDiagnosticAction(anyString(), anyString());

        mockMvc.perform(post("/api/assets/turbine-42/action")
                        .param("action", "LOCK_BRAKES"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.message").value("Kafka cluster unreachable"));
    }

    @Test
    void updateOperatingMode_shouldPublishAndReturnSuccess() throws Exception {
        mockMvc.perform(post("/api/assets/turbine-1/operating-mode")
                        .param("mode", "MAINTENANCE_MODE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.assetId").value("turbine-1"))
                .andExpect(jsonPath("$.operatingMode").value("MAINTENANCE_MODE"));

        verify(eventPublisher).publishOperatingMode("turbine-1", "MAINTENANCE_MODE");
    }

    @Test
    void triggerThermalSpike_shouldPublishAndReturnSuccess() throws Exception {
        mockMvc.perform(post("/api/simulator/spike")
                        .param("assetId", "turbine-5")
                        .param("temperature", "92.4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.assetId").value("turbine-5"))
                .andExpect(jsonPath("$.temperature").value(92.4));

        verify(eventPublisher).publishThermalSpike("turbine-5", 92.4);
    }

    private void performActionAndAssertSuccess(String assetId, String rawAction, String expectedAction) throws Exception {
        mockMvc.perform(post("/api/assets/" + assetId + "/action")
                        .param("action", rawAction))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.assetId").value(assetId))
                .andExpect(jsonPath("$.action").value(expectedAction));

        verify(eventPublisher).publishDiagnosticAction(assetId, expectedAction);
    }
}
