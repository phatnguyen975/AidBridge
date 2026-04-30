package com.drc.aidbridge.modules.sos.internal.web.dto;

import com.drc.aidbridge.modules.shared.enums.UrgencyLevel;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateGuestSosRequest {
    @NotNull
    @DecimalMin("-90.0")
    @DecimalMax("90.0")
    @JsonProperty("lat")
    private Double lat;

    @NotNull
    @DecimalMin("-180.0")
    @DecimalMax("180.0")
    @JsonProperty("lng")
    private Double lng;

    @Size(max = 500)
    @JsonProperty("address")
    private String address;

    @JsonProperty("description")
    private String description;

    @Min(value = 1)
    @Builder.Default
    @JsonProperty("people_count")
    @JsonAlias("peopleCount")
    private Integer peopleCount = 1;

    @NotNull(message = "urgency_level is required")
    @JsonProperty("urgency_level")
    @JsonAlias("urgencyLevel")
    private UrgencyLevel urgencyLevel;

    @JsonProperty("image_url")
    @JsonAlias("imageUrl")
    private String imageUrl;

    @JsonProperty("quick_sos")
    @JsonAlias("quickSos")
    private Boolean quickSos;

    @JsonProperty("triggered_at")
    @JsonAlias("triggeredAt")
    private Instant triggeredAt;

    @JsonProperty("location_captured_at")
    @JsonAlias("locationCapturedAt")
    private Instant locationCapturedAt;

    @DecimalMin("0.0")
    @JsonProperty("accuracy")
    private Double accuracy;

    @Size(max = 100)
    @JsonProperty("client_request_id")
    @JsonAlias("clientRequestId")
    private String clientRequestId;

    @Size(max = 500)
    @JsonProperty("device_info")
    @JsonAlias("deviceInfo")
    private String deviceInfo;

    @Size(max = 30)
    @JsonProperty("source")
    private String source;

    public static CreateGuestSosRequest from(CreateSosRequest request) {
        return CreateGuestSosRequest.builder()
                .lat(request.getLat())
                .lng(request.getLng())
                .address(request.getAddress())
                .description(request.getDescription())
                .peopleCount(request.getPeopleCount())
                .urgencyLevel(request.getUrgencyLevel())
                .imageUrl(request.getImageUrl())
                .quickSos(request.getQuickSos())
                .triggeredAt(request.getTriggeredAt())
                .locationCapturedAt(request.getLocationCapturedAt())
                .accuracy(request.getAccuracy())
                .clientRequestId(request.getClientRequestId())
                .deviceInfo(request.getDeviceInfo())
                .source("APP")
                .build();
    }
}
