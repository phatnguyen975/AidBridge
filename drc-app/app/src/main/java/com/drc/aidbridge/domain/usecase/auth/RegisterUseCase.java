package com.drc.aidbridge.domain.usecase.auth;

import androidx.lifecycle.LiveData;

import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.data.remote.dto.request.RegisterRequest;
import com.drc.aidbridge.domain.enums.UserRole;
import com.drc.aidbridge.domain.repository.AuthRepository;
import com.drc.aidbridge.domain.usecase.common.validation.AuthInputValidator;
import com.drc.aidbridge.domain.usecase.common.validation.ValidationResult;

import javax.inject.Inject;

/**
 * RegisterUseCase — validates and executes user registration.
 *
 * Split into validate() + execute() so the ViewModel can handle
 * validation errors before triggering the network call.
 */
public class RegisterUseCase {

    private final AuthRepository authRepository;
    private final AuthInputValidator inputValidator;

    @Inject
    public RegisterUseCase(AuthRepository authRepository, AuthInputValidator inputValidator) {
        this.authRepository = authRepository;
        this.inputValidator = inputValidator;
    }

    // ------------------------------------------------------------------
    // Validation
    // ------------------------------------------------------------------

    /**
     * Validates registration form inputs.
     *
     * @return ValidationResult (valid=true means all inputs are good)
     */
    public ValidationResult validate(String name, String email, String phone,
                                     String password, UserRole role) {
        ValidationResult nameValidation = inputValidator.requireName(name);
        if (!nameValidation.isValid()) {
            return nameValidation;
        }

        ValidationResult emailValidation = inputValidator.requireValidEmail(email);
        if (!emailValidation.isValid()) {
            return emailValidation;
        }

        ValidationResult phoneValidation = inputValidator.requireValidPhone(phone);
        if (!phoneValidation.isValid()) {
            return phoneValidation;
        }

        ValidationResult passwordValidation = inputValidator.requirePassword(password);
        if (!passwordValidation.isValid()) {
            return passwordValidation;
        }

        return inputValidator.requireRole(role);
    }

    // ------------------------------------------------------------------
    // Execution (called only after validate() returns valid=true)
    // ------------------------------------------------------------------

    /**
     * Delegates to AuthRepository after validation has already passed.
     *
     * @return LiveData emitting Loading → Success(email) | Error(message)
     */
    public LiveData<NetworkResultWrapper<String>> execute(
            String name, String email, String phone, String password, UserRole role) {

        RegisterRequest request = new RegisterRequest(
                inputValidator.normalizeText(name),
                inputValidator.normalizeEmail(email),
                inputValidator.normalizeText(phone),
                password,
                role.name()          // enum → String for the DTO
        );
        return authRepository.register(request);
    }
}

