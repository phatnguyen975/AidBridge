package com.drc.aidbridge.modules.user.internal.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerifyOtpRequest {

    @Email(message = "Invalid email format")
    private String email;

    @JsonProperty("phone_number")
    private String phoneNumber;

    @NotBlank(message = "OTP code is required")
    @Pattern(regexp = "^\\d{6}$", message = "OTP must be 6 digits")
    @JsonProperty("otp_code")
    private String otpCode;

    @NotNull(message = "OTP type is required")
    @Pattern(regexp = "^(EMAIL_VERIFY|PHONE_VERIFY|PASSWORD_RESET)$", message = "OTP type must be EMAIL_VERIFY, PHONE_VERIFY, or PASSWORD_RESET")
    @JsonProperty("otp_type")
    private String otpType;
}
