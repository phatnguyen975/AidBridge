package com.drc.aidbridge.domain.usecase.auth;

import androidx.lifecycle.LiveData;

import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.data.remote.dto.request.OtpVerifyRequest;
import com.drc.aidbridge.data.remote.dto.response.AuthResponse;
import com.drc.aidbridge.domain.repository.AuthRepository;
import com.drc.aidbridge.domain.usecase.validation.AuthInputValidator;
import com.drc.aidbridge.domain.usecase.validation.ValidationResult;

import javax.inject.Inject;

/**
 * VerifyOtpUseCase — validates OTP format and delegates to AuthRepository.
 */
public class VerifyOtpUseCase {

    private final AuthRepository authRepository;
    private final AuthInputValidator inputValidator;

    @Inject
    public VerifyOtpUseCase(AuthRepository authRepository, AuthInputValidator inputValidator) {
        this.authRepository = authRepository;
        this.inputValidator = inputValidator;
    }

    public ValidationResult validate(String otp) {
        return inputValidator.requireOtp(otp);
    }

    public LiveData<NetworkResultWrapper<AuthResponse>> execute(String email, String otpCode) {
        return authRepository.verifyOtp(
                new OtpVerifyRequest(inputValidator.normalizeEmail(email), otpCode));
    }
}
