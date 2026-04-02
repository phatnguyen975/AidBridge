package com.drc.aidbridge.modules.user.internal.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO cho yêu cầu gửi OTP.
 * Khớp với RequestOtpRequest schema trong api.yaml.
 * Thay thế ResendOtpRequest và ForgotPasswordRequest.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestOtpRequest {

    @Email(message = "Invalid email format")
    private String email;

    @JsonProperty("phone_number")
    private String phoneNumber;

    @NotNull(message = "OTP type is required")
    @Pattern(regexp = "^(EMAIL_VERIFY|PHONE_VERIFY|PASSWORD_RESET)$", message = "OTP type must be EMAIL_VERIFY, PHONE_VERIFY, or PASSWORD_RESET")
    @JsonProperty("otp_type")
    private String otpType;
}
