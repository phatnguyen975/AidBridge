package com.drc.aidbridge.modules.mission.internal.web.dto;

import com.drc.aidbridge.modules.shared.enums.DispatchResponse;
import com.drc.aidbridge.modules.shared.enums.DispatchType;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO cho dispatch attempt.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DispatchAttemptResponse {

    private UUID id;
    private UUID missionId;
    private UUID volunteerId;
    private DispatchType dispatchType;
    private Integer batchNumber;
    private BigDecimal radiusKm;
    private DispatchResponse response;
    private Instant respondedAt;
    private Instant createdAt;

    // Thông tin volunteer (nếu có)
    private VolunteerBrief volunteer;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VolunteerBrief {
        private UUID id;
        private String fullName;
        private String phoneNumber;
        private String avatarUrl;
        private Double avgRating;
        private Integer totalTasksCompleted;
    }
}
