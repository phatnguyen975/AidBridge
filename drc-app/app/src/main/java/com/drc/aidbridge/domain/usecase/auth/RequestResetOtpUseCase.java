package com.drc.aidbridge.domain.usecase.auth;

import androidx.lifecycle.LiveData;

import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.data.remote.dto.request.ForgotPasswordRequest;
import com.drc.aidbridge.domain.repository.AuthRepository;
import com.drc.aidbridge.domain.usecase.validation.AuthInputValidator;
import com.drc.aidbridge.domain.usecase.validation.ValidationResult;

import javax.inject.Inject;

/**
 * Requests a reset OTP after validating email format.
 */
public class RequestResetOtpUseCase {

    private final AuthRepository authRepository;
    private final AuthInputValidator inputValidator;

    @Inject
    public RequestResetOtpUseCase(AuthRepository authRepository,
                                  AuthInputValidator inputValidator) {
        this.authRepository = authRepository;
        this.inputValidator = inputValidator;
    }

    public ValidationResult validate(String email) {
        return inputValidator.requireValidEmail(email);
    }

    public LiveData<NetworkResultWrapper<String>> execute(String email) {
        return authRepository.forgotPassword(
                new ForgotPasswordRequest(inputValidator.normalizeEmail(email)));
    }
}
