package com.drc.aidbridge.modules.sos.internal.web.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SmsIngestSosRequest {

    @NotBlank
    @Size(max = 100)
    @JsonProperty("clientRequestId")
    @JsonAlias("client_request_id")
    private String clientRequestId;

    @Size(max = 50)
    @JsonProperty("senderPhone")
    @JsonAlias({"sender_phone", "phone"})
    private String senderPhone;

    @NotNull
    @DecimalMin("-90.0")
    @DecimalMax("90.0")
    @JsonProperty("latitude")
    @JsonAlias("lat")
    private Double latitude;

    @NotNull
    @DecimalMin("-180.0")
    @DecimalMax("180.0")
    @JsonProperty("longitude")
    @JsonAlias("lng")
    private Double longitude;

    @DecimalMin("0.0")
    @JsonProperty("accuracy")
    private Double accuracy;

    @JsonProperty("triggeredAtMillis")
    @JsonAlias("triggered_at_millis")
    private Long triggeredAtMillis;

    @JsonProperty("locationCapturedAtMillis")
    @JsonAlias("location_captured_at_millis")
    private Long locationCapturedAtMillis;

    @Min(1)
    @JsonProperty("peopleCount")
    @JsonAlias("people_count")
    private Integer peopleCount;

    @JsonProperty("quickSos")
    @JsonAlias("quick_sos")
    private Boolean quickSos;

    @Size(max = 1000)
    @JsonProperty("rawMessage")
    @JsonAlias("raw_message")
    private String rawMessage;

    @JsonProperty("receivedAtGatewayMillis")
    @JsonAlias("received_at_gateway_millis")
    private Long receivedAtGatewayMillis;
}
