package com.drc.aidbridge.domain.usecase.auth;

import androidx.lifecycle.LiveData;

import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.data.remote.dto.request.ResetPasswordRequest;
import com.drc.aidbridge.domain.repository.AuthRepository;
import com.drc.aidbridge.domain.usecase.validation.AuthInputValidator;
import com.drc.aidbridge.domain.usecase.validation.ValidationResult;

import javax.inject.Inject;

/**
 * Resets password after OTP verification.
 */
public class ResetPasswordUseCase {

    private final AuthRepository authRepository;
    private final AuthInputValidator inputValidator;

    @Inject
    public ResetPasswordUseCase(AuthRepository authRepository,
                                AuthInputValidator inputValidator) {
        this.authRepository = authRepository;
        this.inputValidator = inputValidator;
    }

    public ValidationResult validate(String email, String newPassword, String confirmPassword) {
        ValidationResult emailValidation = inputValidator.requireValidEmail(email);
        if (!emailValidation.isValid()) {
            return emailValidation;
        }
        ValidationResult passwordValidation = inputValidator.requirePassword(newPassword);
        if (!passwordValidation.isValid()) {
            return passwordValidation;
        }
        return inputValidator.requirePasswordMatch(newPassword, confirmPassword);
    }

    public LiveData<NetworkResultWrapper<String>> execute(String email, String newPassword) {
        return authRepository.resetPassword(
                new ResetPasswordRequest(inputValidator.normalizeEmail(email), newPassword));
    }
}
