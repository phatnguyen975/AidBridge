package com.drc.aidbridge.modules.user.internal.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 100, message = "Full name must be 2-100 characters")
    @JsonProperty("full_name")
    private String fullName;

    @Email(message = "Invalid email format")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    private String email;

    @JsonProperty("phone_number")
    @Size(max = 20, message = "Phone number must not exceed 20 characters")
    private String phoneNumber;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    @NotNull(message = "Role is required")
    @Pattern(regexp = "^(VICTIM|VOLUNTEER|SPONSOR)$", message = "Role must be VICTIM, VOLUNTEER, or SPONSOR")
    private String role;

    @JsonProperty("avatar_url")
    @Size(max = 500, message = "Avatar URL must not exceed 500 characters")
    private String avatarUrl;

    @JsonProperty("victim_details")
    private VictimDetails victimDetails;

    @JsonProperty("volunteer_details")
    private VolunteerDetails volunteerDetails;

    @JsonProperty("sponsor_details")
    private SponsorDetails sponsorDetails;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VictimDetails {
        private Double latitude;
        private Double longitude;
        private String address;
        @JsonProperty("household_size")
        private Integer householdSize;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VolunteerDetails {
        @JsonProperty("vehicle_type")
        private String vehicleType;
        @JsonProperty("id_card_number")
        private String idCardNumber;
        @JsonProperty("emergency_contact")
        private String emergencyContact;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SponsorDetails {
        @JsonProperty("organization_name")
        private String organizationName;
        @JsonProperty("tax_id")
        private String taxId;
        @JsonProperty("contact_person")
        private String contactPerson;
    }
}
