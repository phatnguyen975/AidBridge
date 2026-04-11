package com.drc.aidbridge.domain.usecase.user;

import androidx.lifecycle.LiveData;

import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.domain.repository.UserRepository;
import com.drc.aidbridge.domain.usecase.validation.AuthInputValidator;
import com.drc.aidbridge.domain.usecase.validation.AuthValidationResult;

import javax.inject.Inject;

public class ChangePasswordUseCase {

    private final UserRepository userRepository;
    private final AuthInputValidator inputValidator;

    @Inject
    public ChangePasswordUseCase(UserRepository userRepository,
                                 AuthInputValidator inputValidator) {
        this.userRepository = userRepository;
        this.inputValidator = inputValidator;
    }

    public AuthValidationResult validate(String currentPassword,
                                     String newPassword,
                                     String confirmNewPassword) {
        AuthValidationResult currentPasswordValidation = inputValidator.requirePassword(currentPassword);
        if (!currentPasswordValidation.isValid()) {
            return AuthValidationResult.invalid(AuthValidationResult.Field.PASSWORD, "Mat khau hien tai khong hop le.");
        }

        AuthValidationResult newPasswordValidation = inputValidator.requirePassword(newPassword);
        if (!newPasswordValidation.isValid()) {
            return newPasswordValidation;
        }

        return inputValidator.requirePasswordMatch(newPassword, confirmNewPassword);
    }

    public LiveData<NetworkResultWrapper<String>> execute(String currentPassword, String newPassword) {
        return userRepository.changePassword(currentPassword, newPassword);
    }
}

