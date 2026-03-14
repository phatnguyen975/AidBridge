package com.drc.aidbridge.domain.usecase.auth;

import androidx.lifecycle.LiveData;

import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.data.remote.dto.request.OtpVerifyRequest;
import com.drc.aidbridge.domain.repository.AuthRepository;
import com.drc.aidbridge.domain.usecase.common.validation.AuthInputValidator;
import com.drc.aidbridge.domain.usecase.common.validation.ValidationResult;

import javax.inject.Inject;

/**
 * Verifies reset-password OTP with strict OTP format checks.
 */
public class VerifyResetOtpUseCase {

    private final AuthRepository authRepository;
    private final AuthInputValidator inputValidator;

    @Inject
    public VerifyResetOtpUseCase(AuthRepository authRepository,
                                 AuthInputValidator inputValidator) {
        this.authRepository = authRepository;
        this.inputValidator = inputValidator;
    }

    public ValidationResult validate(String email, String otpCode) {
        ValidationResult emailValidation = inputValidator.requireValidEmail(email);
        if (!emailValidation.isValid()) {
            return emailValidation;
        }
        return inputValidator.requireOtp(otpCode);
    }

    public LiveData<NetworkResultWrapper<String>> execute(String email, String otpCode) {
        return authRepository.verifyResetOtp(
                new OtpVerifyRequest(inputValidator.normalizeEmail(email), otpCode));
    }
}
