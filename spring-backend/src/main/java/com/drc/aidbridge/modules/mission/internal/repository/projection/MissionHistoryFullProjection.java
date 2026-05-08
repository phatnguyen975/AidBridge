package com.drc.aidbridge.modules.mission.internal.repository.projection;

import com.drc.aidbridge.modules.shared.enums.MissionStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public interface MissionHistoryFullProjection extends MissionHistoryProjection {
    
    // Thuộc tính của bảng mission
    UUID getId();
    UUID getSosRequestId();
    UUID getAidRequestId();
    UUID getVolunteerId();
    UUID getHubId();
    String getCodeName();
    MissionStatus getStatus();
    String getQrCodeToken();
    BigDecimal getPriorityScore();
    Instant getAcceptedAt();
    Instant getPickedUpAt();
    Instant getStartedAt();
    Instant getCancelledAt();
    String getCancellationReason();
    String getConfirmationImageUrl();
    String getImageUrl();
    String getComment();
    Instant getCreatedAt();
    Instant getUpdatedAt();
    Double getVictimLat();
    Double getVictimLng();

    // Thuộc tính mở rộng từ MissionHistoryProjection đã có sẵn:
    // - String getMissionType()
    // - Instant getCompletedAt()
    // - String getAddress()
    Double getRadiusKm();
    String getDescription();
}
