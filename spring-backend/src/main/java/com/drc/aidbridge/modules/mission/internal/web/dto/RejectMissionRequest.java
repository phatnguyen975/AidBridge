package com.drc.aidbridge.modules.mission.internal.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Request DTO cho reject mission (volunteer từ chối dispatch).
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

    /**
     * Lý do từ chối
     */
    @NotBlank(message = "Reason is required")
    private String reason;

    /**
     * Chi tiết lý do (nếu reason = OTHER)
     */
    private String reasonDetail;

    /**
     * Enum định nghĩa các lý do từ chối phổ biến
     */
    public enum RejectReason {
        BUSY, // Đang bận
        TOO_FAR, // Quá xa
        PERSONAL, // Lý do cá nhân
        OTHER // Lý do khác
    }
}
