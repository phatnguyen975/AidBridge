package com.drc.aidbridge.modules.hub.internal.repository.projection;

import java.util.UUID;
import java.time.Instant;

public interface HubSearchResultProjection {
    UUID getId();
    String getName();
    String getAddress();
    String getPhoneNumber();
    String getImageUrl();
    String getStatus();
    String getOperatingHours();
    Instant getCreatedAt();
    Instant getUpdatedAt();
    Double getLatitude();
    Double getLongitude();
    Double getDistanceInMeters();
}
