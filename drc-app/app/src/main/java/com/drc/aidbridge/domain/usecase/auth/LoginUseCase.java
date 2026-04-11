package com.drc.aidbridge.domain.usecase.auth;

import androidx.lifecycle.LiveData;

import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.data.remote.dto.request.LoginRequest;
import com.drc.aidbridge.domain.model.User;
import com.drc.aidbridge.domain.repository.AuthRepository;
import com.drc.aidbridge.domain.usecase.validation.AuthInputValidator;
import com.drc.aidbridge.domain.usecase.validation.ValidationResult;

import javax.inject.Inject;

/**
 * LoginUseCase - validates and executes user login.
 */
public class LoginUseCase {

    private final AuthRepository authRepository;
    private final AuthInputValidator inputValidator;

    @Inject
    public LoginUseCase(AuthRepository authRepository, AuthInputValidator inputValidator) {
        this.authRepository = authRepository;
        this.inputValidator = inputValidator;
    }

    public ValidationResult validate(String email, String password) {
        ValidationResult emailValidation = inputValidator.requireValidEmail(email);
        if (!emailValidation.isValid()) {
            return emailValidation;
        }
        return inputValidator.requirePassword(password);
    }

    public LiveData<NetworkResultWrapper<User>> execute(String email,
                                                        String password,
                                                        String deviceId,
                                                        String fcmToken) {
        return authRepository.login(
            new LoginRequest(
                inputValidator.normalizeEmail(email),
                password,
                normalizeOptional(deviceId),
                normalizeOptional(fcmToken)
            )
        );
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}

