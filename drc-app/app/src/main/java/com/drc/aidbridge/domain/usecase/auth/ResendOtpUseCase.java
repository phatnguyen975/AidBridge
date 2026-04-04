package com.drc.aidbridge.domain.usecase.auth;

import androidx.lifecycle.LiveData;

import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.data.remote.dto.request.RequestOtpRequest;
import com.drc.aidbridge.domain.repository.AuthRepository;
import com.drc.aidbridge.domain.usecase.validation.AuthInputValidator;
import com.drc.aidbridge.domain.usecase.validation.ValidationResult;

import javax.inject.Inject;

/**
 * Shared OTP resend logic for both register OTP and forgot-password OTP flows.
 */
public class ResendOtpUseCase {

    private final AuthRepository authRepository;
    private final AuthInputValidator inputValidator;

    @Inject
    public ResendOtpUseCase(AuthRepository authRepository,
            AuthInputValidator inputValidator) {
        this.authRepository = authRepository;
        this.inputValidator = inputValidator;
    }

    public ValidationResult validate(String email) {
        return inputValidator.requireValidEmail(email);
    }

    public LiveData<NetworkResultWrapper<Boolean>> execute(String email, String otpType) {
        return authRepository.resendOtp(
                new RequestOtpRequest(inputValidator.normalizeEmail(email), null, otpType));
    }
}
