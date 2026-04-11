package com.drc.aidbridge.domain.usecase.user;

import androidx.lifecycle.LiveData;

import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.domain.repository.UserRepository;
import com.drc.aidbridge.domain.usecase.validation.AuthInputValidator;
import com.drc.aidbridge.domain.usecase.validation.ValidationResult;

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

    public ValidationResult validate(String currentPassword,
                                     String newPassword,
                                     String confirmNewPassword) {
        ValidationResult currentPasswordValidation = inputValidator.requirePassword(currentPassword);
        if (!currentPasswordValidation.isValid()) {
            return ValidationResult.invalid(ValidationResult.Field.PASSWORD, "Mật khẩu hiện tại không hợp lệ.");
        }

        ValidationResult newPasswordValidation = inputValidator.requirePassword(newPassword);
        if (!newPasswordValidation.isValid()) {
            return newPasswordValidation;
        }

        return inputValidator.requirePasswordMatch(newPassword, confirmNewPassword);
    }

    public LiveData<NetworkResultWrapper<String>> execute(String currentPassword, String newPassword) {
        return userRepository.changePassword(currentPassword, newPassword);
    }
}

