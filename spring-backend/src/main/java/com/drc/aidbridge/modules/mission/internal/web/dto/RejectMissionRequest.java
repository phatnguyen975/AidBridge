package com.drc.aidbridge.modules.mission.internal.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Request DTO cho reject mission
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RejectMissionRequest {

    /**
     * ID của dispatch attempt đang được respond
     */
    private UUID dispatchAttemptId;

    @NotBlank(message = "Reason is required")
    private String reason;

    /**
     * reason = OTHER
     */
    private String reasonDetail;

    /**
     * Enum
     */
    public enum RejectReason {
        BUSY,
        TOO_FAR,
        PERSONAL,
        OTHER
    }
}
