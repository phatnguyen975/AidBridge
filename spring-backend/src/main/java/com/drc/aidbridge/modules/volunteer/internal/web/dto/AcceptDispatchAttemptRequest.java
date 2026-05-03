package com.drc.aidbridge.modules.volunteer.internal.web.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AcceptDispatchAttemptRequest {
    @NotNull(message = "Dispatch attempt ID is required")
    private UUID dispatchAttemptId;
}
