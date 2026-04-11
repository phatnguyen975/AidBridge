package com.drc.aidbridge.domain.usecase.validation;

import com.drc.aidbridge.domain.enums.UserRole;
import com.drc.aidbridge.utils.Constants;

import javax.inject.Inject;

/**
 * Centralized validator for auth-related user input.
 */
public class AuthInputValidator {

    @Inject
    public AuthInputValidator() {
    }

    public String normalizeText(String value) {
        return value != null ? value.trim() : "";
    }

    public String normalizeEmail(String email) {
        return normalizeText(email).toLowerCase();
    }

    public ValidationResult requireValidEmail(String email) {
        String normalizedEmail = normalizeEmail(email);
        if (normalizedEmail.isEmpty()) {
            return ValidationResult.invalid(ValidationResult.Field.EMAIL, "Email không được để trống.");
        }
        if (!Constants.EMAIL_PATTERN.matcher(normalizedEmail).matches()) {
            return ValidationResult.invalid(ValidationResult.Field.EMAIL, "Địa chỉ email không hợp lệ.");
        }
        return ValidationResult.valid();
    }

    public ValidationResult requireName(String name) {
        if (normalizeText(name).isEmpty()) {
            return ValidationResult.invalid(ValidationResult.Field.NAME, "Họ tên không được để trống.");
        }
        return ValidationResult.valid();
    }

    public ValidationResult requireValidPhone(String phone) {
        String normalizedPhone = normalizeText(phone);
        if (normalizedPhone.isEmpty()) {
            return ValidationResult.invalid(ValidationResult.Field.PHONE, "Số điện thoại không được để trống.");
        }
        if (!Constants.PHONE_PATTERN.matcher(normalizedPhone).matches()) {
            return ValidationResult.invalid(ValidationResult.Field.PHONE, "Vui lòng nhập số điện thoại hợp lệ (10 chữ số).");
        }
        return ValidationResult.valid();
    }

    public ValidationResult requirePassword(String password) {
        if (password.isEmpty()) {
            return ValidationResult.invalid(ValidationResult.Field.PASSWORD, "Mật khẩu không được để trống.");
        }
        if (!Constants.PASSWORD_PATTERN.matcher(password).matches()) {
            return ValidationResult.invalid(ValidationResult.Field.PASSWORD, "Mật khẩu phải có ít nhất 6 ký tự, bao gồm chữ cái và số.");
        }
        return ValidationResult.valid();
    }

    public ValidationResult requirePasswordMatch(String password, String confirmPassword) {
        if (confirmPassword.isEmpty()) {
            return ValidationResult.invalid(ValidationResult.Field.CONFIRM_PASSWORD, "Vui lòng nhập lại mật khẩu.");
        }
        if (!password.equals(confirmPassword)) {
            return ValidationResult.invalid(ValidationResult.Field.CONFIRM_PASSWORD, "Mật khẩu xác nhận không khớp.");
        }
        return ValidationResult.valid();
    }

    public ValidationResult requireRole(UserRole role) {
        if (role == null) {
            return ValidationResult.invalid(ValidationResult.Field.ROLE, "Vui lòng chọn vai trò của bạn.");
        }
        return ValidationResult.valid();
    }

    public ValidationResult requireOtp(String otpCode) {
        String normalizedOtp = normalizeText(otpCode);
        if (normalizedOtp.isEmpty()) {
            return ValidationResult.invalid(ValidationResult.Field.OTP, "Mã OTP không được để trống.");
        }
        if (!Constants.OTP_PATTERN.matcher(normalizedOtp).matches()) {
            return ValidationResult.invalid(ValidationResult.Field.OTP, "Vui lòng nhập mã OTP hợp lệ (6 chữ số).");
        }
        return ValidationResult.valid();
    }
}
