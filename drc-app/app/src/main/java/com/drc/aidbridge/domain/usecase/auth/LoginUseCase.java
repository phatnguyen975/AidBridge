package com.drc.aidbridge.domain.usecase.auth;

import androidx.lifecycle.LiveData;

import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.data.remote.dto.request.LoginRequest;
import com.drc.aidbridge.domain.model.User;
import com.drc.aidbridge.domain.repository.AuthRepository;
import com.drc.aidbridge.domain.usecase.common.validation.AuthInputValidator;
import com.drc.aidbridge.domain.usecase.common.validation.ValidationResult;

import javax.inject.Inject;

/**
 * LoginUseCase — encapsulates the business logic for user authentication.
 *
 * Responsibilities:
 * 1. Basic input validation (email format, password length).
 * 2. Delegates the actual API call to AuthRepository.
 *
 * Why UseCase? It keeps validation logic out of the ViewModel and makes
 * it independently testable without Android dependencies.
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

    public LiveData<NetworkResultWrapper<User>> execute(String email, String password) {
        String normalizedEmail = inputValidator.normalizeEmail(email);
        return authRepository.login(new LoginRequest(normalizedEmail, password));
    }
}
