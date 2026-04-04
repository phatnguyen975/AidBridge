package com.drc.aidbridge.domain.usecase.user;

import androidx.lifecycle.LiveData;

import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.domain.model.User;
import com.drc.aidbridge.domain.repository.UserRepository;
import com.drc.aidbridge.domain.usecase.validation.AuthInputValidator;
import com.drc.aidbridge.domain.usecase.validation.ValidationResult;

import javax.inject.Inject;

public class UpdateProfileUseCase {

    private final UserRepository userRepository;
    private final AuthInputValidator inputValidator;

    @Inject
    public UpdateProfileUseCase(UserRepository userRepository,
                                AuthInputValidator inputValidator) {
        this.userRepository = userRepository;
        this.inputValidator = inputValidator;
    }

    public ValidationResult validate(String name, String phone) {
        ValidationResult nameValidation = inputValidator.requireName(name);
        if (!nameValidation.isValid()) {
            return nameValidation;
        }

        return inputValidator.requireValidPhone(phone);
    }

    public LiveData<NetworkResultWrapper<User>> execute(String name, String phone, String address) {
        return userRepository.updateProfile(
            inputValidator.normalizeText(name),
            inputValidator.normalizeText(phone),
            inputValidator.normalizeText(address)
        );
    }
}
