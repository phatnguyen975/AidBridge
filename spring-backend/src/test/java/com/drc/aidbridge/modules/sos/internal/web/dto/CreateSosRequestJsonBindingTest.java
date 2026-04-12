package com.drc.aidbridge.modules.sos.internal.web.dto;

import com.drc.aidbridge.modules.shared.enums.UrgencyLevel;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreateSosRequestJsonBindingTest {

    private ObjectMapper objectMapper;
    private Validator validator;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void shouldBindSnakeCaseUrgencyLevel_WhenPayloadContainsUrgencyLevel() throws Exception {
        String payload = """
                {
                  \"lat\": -90,
                  \"lng\": -180,
                  \"address\": \"string\",
                  \"description\": \"string\",
                  \"people_count\": 1,
                  \"urgency_level\": \"CRITICAL\",
                  \"image_url\": \"string\"
                }
                """;

        CreateSosRequest request = objectMapper.readValue(payload, CreateSosRequest.class);

        assertEquals(UrgencyLevel.CRITICAL, request.getUrgencyLevel());
        assertEquals(Integer.valueOf(1), request.getPeopleCount());
    }

    @Test
    void shouldFailValidation_WhenUrgencyLevelIsMissing() throws Exception {
        String payloadWithoutUrgency = """
                {
                  \"lat\": -90,
                  \"lng\": -180,
                  \"address\": \"string\",
                  \"description\": \"string\",
                  \"people_count\": 1,
                  \"image_url\": \"string\"
                }
                """;

        CreateSosRequest request = objectMapper.readValue(payloadWithoutUrgency, CreateSosRequest.class);
        Set<ConstraintViolation<CreateSosRequest>> violations = validator.validate(request);

        assertTrue(violations.stream().anyMatch(v -> "urgencyLevel".equals(v.getPropertyPath().toString())));
    }
}
