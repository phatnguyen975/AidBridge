package com.drc.aidbridge.domain.usecase.auth;

import androidx.lifecycle.LiveData;

import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.data.remote.dto.request.RegisterRequest;
import com.drc.aidbridge.domain.enums.UserRole;
import com.drc.aidbridge.domain.model.User;
import com.drc.aidbridge.domain.repository.AuthRepository;
import com.drc.aidbridge.domain.usecase.validation.AuthInputValidator;
import com.drc.aidbridge.domain.usecase.validation.AuthValidationResult;

import javax.inject.Inject;

/**
 * RegisterUseCase â€” validates and executes user registration.
 */
public class RegisterUseCase {

    private final AuthRepository authRepository;
    private final AuthInputValidator inputValidator;

    @Inject
    public RegisterUseCase(AuthRepository authRepository, AuthInputValidator inputValidator) {
        this.authRepository = authRepository;
        this.inputValidator = inputValidator;
    }

    public AuthValidationResult validate(String name, String email, String phone,
                                     String password, UserRole role) {
        AuthValidationResult nameValidation = inputValidator.requireName(name);
        if (!nameValidation.isValid()) {
            return nameValidation;
        }

        AuthValidationResult emailValidation = inputValidator.requireValidEmail(email);
        if (!emailValidation.isValid()) {
            return emailValidation;
        }

        AuthValidationResult phoneValidation = inputValidator.requireValidPhone(phone);
        if (!phoneValidation.isValid()) {
            return phoneValidation;
        }

        AuthValidationResult passwordValidation = inputValidator.requirePassword(password);
        if (!passwordValidation.isValid()) {
            return passwordValidation;
        }

        return inputValidator.requireRole(role);
    }

    public LiveData<NetworkResultWrapper<User>> execute(
            String name, String email, String phone, String password, UserRole role) {
        return authRepository.register(new RegisterRequest(
                inputValidator.normalizeText(name),
                inputValidator.normalizeEmail(email),
                inputValidator.normalizeText(phone),
                password,
                role.name()
            ));
    }
}

