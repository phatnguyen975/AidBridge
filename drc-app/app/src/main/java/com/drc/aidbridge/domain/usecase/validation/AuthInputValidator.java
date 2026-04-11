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

    public AuthValidationResult requireValidEmail(String email) {
        String normalizedEmail = normalizeEmail(email);
        if (normalizedEmail.isEmpty()) {
            return AuthValidationResult.invalid(AuthValidationResult.Field.EMAIL, "Email khong duoc de trong.");
        }
        if (!Constants.EMAIL_PATTERN.matcher(normalizedEmail).matches()) {
            return AuthValidationResult.invalid(AuthValidationResult.Field.EMAIL, "Dia chi email khong hop le.");
        }
        return AuthValidationResult.valid();
    }

    public AuthValidationResult requireName(String name) {
        if (normalizeText(name).isEmpty()) {
            return AuthValidationResult.invalid(AuthValidationResult.Field.NAME, "Ho ten khong duoc de trong.");
        }
        return AuthValidationResult.valid();
    }

    public AuthValidationResult requireValidPhone(String phone) {
        String normalizedPhone = normalizeText(phone);
        if (normalizedPhone.isEmpty()) {
            return AuthValidationResult.invalid(AuthValidationResult.Field.PHONE, "So dien thoai khong duoc de trong.");
        }
        if (!Constants.PHONE_PATTERN.matcher(normalizedPhone).matches()) {
            return AuthValidationResult.invalid(AuthValidationResult.Field.PHONE, "Vui long nhap so dien thoai hop le (10 chu so).");
        }
        return AuthValidationResult.valid();
    }

    public AuthValidationResult requirePassword(String password) {
        if (password.isEmpty()) {
            return AuthValidationResult.invalid(AuthValidationResult.Field.PASSWORD, "Mat khau khong duoc de trong.");
        }
        if (!Constants.PASSWORD_PATTERN.matcher(password).matches()) {
            return AuthValidationResult.invalid(AuthValidationResult.Field.PASSWORD, "Mat khau phai co it nhat 6 ky tu, bao gom chu cai va so.");
        }
        return AuthValidationResult.valid();
    }

    public AuthValidationResult requirePasswordMatch(String password, String confirmPassword) {
        if (confirmPassword.isEmpty()) {
            return AuthValidationResult.invalid(AuthValidationResult.Field.CONFIRM_PASSWORD, "Vui long nhap lai mat khau.");
        }
        if (!password.equals(confirmPassword)) {
            return AuthValidationResult.invalid(AuthValidationResult.Field.CONFIRM_PASSWORD, "Mat khau xac nhan khong khop.");
        }
        return AuthValidationResult.valid();
    }

    public AuthValidationResult requireRole(UserRole role) {
        if (role == null) {
            return AuthValidationResult.invalid(AuthValidationResult.Field.ROLE, "Vui long chon vai tro cua ban.");
        }
        return AuthValidationResult.valid();
    }

    public AuthValidationResult requireOtp(String otpCode) {
        String normalizedOtp = normalizeText(otpCode);
        if (normalizedOtp.isEmpty()) {
            return AuthValidationResult.invalid(AuthValidationResult.Field.OTP, "Ma OTP khong duoc de trong.");
        }
        if (!Constants.OTP_PATTERN.matcher(normalizedOtp).matches()) {
            return AuthValidationResult.invalid(AuthValidationResult.Field.OTP, "Vui long nhap ma OTP hop le (6 chu so).");
        }
        return AuthValidationResult.valid();
    }
}

