package com.drc.aidbridge.domain.usecase.common.validation;

import com.drc.aidbridge.domain.enums.UserRole;
import com.drc.aidbridge.utils.Constants;

import java.util.regex.Pattern;

import javax.inject.Inject;

/**
 * Centralized validator for auth-related user input.
 */
public class AuthInputValidator {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\\\.[A-Z]{2,}$",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern OTP_PATTERN = Pattern.compile("^[0-9]+$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^0[0-9]{9,10}$");

    @Inject
    public AuthInputValidator() {
    }

    public String normalizeEmail(String email) {
        return email != null ? email.trim().toLowerCase() : "";
    }

    public String normalizeText(String value) {
        return value != null ? value.trim() : "";
    }

    public ValidationResult requireValidEmail(String email) {
        String normalizedEmail = normalizeEmail(email);
        if (normalizedEmail.isEmpty()) {
            return ValidationResult.invalid("Email không được để trống.");
        }
        if (!EMAIL_PATTERN.matcher(normalizedEmail).matches()) {
            return ValidationResult.invalid("Địa chỉ email không hợp lệ.");
        }
        return ValidationResult.valid();
    }

    public ValidationResult requireName(String name) {
        if (normalizeText(name).isEmpty()) {
            return ValidationResult.invalid("Họ và tên không được để trống.");
        }
        return ValidationResult.valid();
    }

    public ValidationResult requireValidPhone(String phone) {
        String normalizedPhone = normalizeText(phone);
        if (normalizedPhone.isEmpty()) {
            return ValidationResult.invalid("Số điện thoại không được để trống.");
        }
        if (!PHONE_PATTERN.matcher(normalizedPhone).matches()) {
            return ValidationResult.invalid("Số điện thoại không hợp lệ (phải bắt đầu bằng 0, 10-11 chữ số).");
        }
        return ValidationResult.valid();
    }

    public ValidationResult requirePassword(String password) {
        if (password == null || password.isEmpty()) {
            return ValidationResult.invalid("Mật khẩu không được để trống.");
        }
        if (password.length() < Constants.PASSWORD_MIN_LENGTH) {
            return ValidationResult.invalid(
                    "Mật khẩu phải có ít nhất " + Constants.PASSWORD_MIN_LENGTH + " ký tự.");
        }
        return ValidationResult.valid();
    }

    public ValidationResult requireRole(UserRole role) {
        if (role == null) {
            return ValidationResult.invalid("Vui lòng chọn vai trò của bạn.");
        }
        return ValidationResult.valid();
    }

    public ValidationResult requireOtp(String otpCode) {
        if (otpCode == null || otpCode.length() != Constants.OTP_LENGTH
                || !OTP_PATTERN.matcher(otpCode).matches()) {
            return ValidationResult.invalid("Vui lòng nhập đủ 6 chữ số OTP.");
        }
        return ValidationResult.valid();
    }
}
