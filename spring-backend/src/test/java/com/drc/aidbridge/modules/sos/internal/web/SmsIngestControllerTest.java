package com.drc.aidbridge.modules.sos.internal.web;

import com.drc.aidbridge.modules.sos.internal.usecase.CreateSmsIngestSosUseCase;
import com.drc.aidbridge.modules.sos.internal.web.dto.SosRequestResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SmsIngestControllerTest {

    private CreateSmsIngestSosUseCase useCase;
    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        useCase = mock(CreateSmsIngestSosUseCase.class);
        SmsIngestController controller = new SmsIngestController(useCase);
        ReflectionTestUtils.setField(controller, "gatewaySmsToken", "test-token");
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void ingestSos_ShouldReturnCreated_WhenGatewayTokenIsValid() throws Exception {
        when(useCase.execute(any())).thenReturn(SosRequestResponse.builder()
            .id(UUID.randomUUID())
            .clientRequestId("TEST-123")
            .source("SMS")
            .quickSos(true)
            .build());

        mockMvc.perform(post("/api/sms-ingest/sos")
                .header("X-Gateway-Token", "test-token")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(sampleBody())))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.clientRequestId").value("TEST-123"))
            .andExpect(jsonPath("$.data.source").value("SMS"));

        verify(useCase).execute(any());
    }

    @Test
    void ingestSos_ShouldReturnUnauthorized_WhenGatewayTokenIsInvalid() throws Exception {
        mockMvc.perform(post("/api/sms-ingest/sos")
                .header("X-Gateway-Token", "wrong-token")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(sampleBody())))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.success").value(false));

        verify(useCase, never()).execute(any());
    }

    private Map<String, Object> sampleBody() {
        return Map.of(
            "clientRequestId", "TEST-123",
            "senderPhone", "0901234567",
            "latitude", 10.762622,
            "longitude", 106.660172,
            "accuracy", 12.5,
            "triggeredAtMillis", 1710000000000L,
            "peopleCount", 1,
            "quickSos", true,
            "rawMessage", "AIDBRIDGE_SOS|id=TEST-123|lat=10.762622|lng=106.660172",
            "receivedAtGatewayMillis", 1710000000100L
        );
    }
}
