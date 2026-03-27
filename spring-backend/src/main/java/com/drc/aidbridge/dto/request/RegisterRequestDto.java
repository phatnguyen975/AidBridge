package com.drc.aidbridge.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for user registration.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequestDto {

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100, message = "Name must be 2-100 characters")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @Pattern(regexp = "^\\d{10,11}$", message = "Phone must be 10-11 digits")
    private String phone;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    @NotBlank(message = "Role is required")
    @Pattern(regexp = "^(VICTIM|VOLUNTEER|SPONSOR)$", message = "Role must be VICTIM, VOLUNTEER, or SPONSOR")
    private String role;

    // Role-specific fields (optional, validated in service layer)
    private VictimDetails victimDetails;
    private VolunteerDetails volunteerDetails;
    private SponsorDetails sponsorDetails;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VictimDetails {
        private Double latitude;
        private Double longitude;
        private String address;
        private Integer householdSize;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VolunteerDetails {
        private String vehicleType; // MOTORBIKE, CAR, BICYCLE, WALKING
        private String idCardNumber;
        private String emergencyContact;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SponsorDetails {
        private String organizationName;
        private String taxId;
        private String contactPerson;
    }
}
