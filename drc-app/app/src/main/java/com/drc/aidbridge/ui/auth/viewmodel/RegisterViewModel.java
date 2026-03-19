package com.drc.aidbridge.ui.auth.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.domain.enums.UserRole;
import com.drc.aidbridge.domain.usecase.auth.RegisterUseCase;
import com.drc.aidbridge.domain.usecase.validation.ValidationResult;
import com.drc.aidbridge.ui.base.BaseViewModel;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class RegisterViewModel extends BaseViewModel {

    private final RegisterUseCase registerUseCase;

    private final MutableLiveData<UserRole> selectedRole = new MutableLiveData<>(null);
    private final MutableLiveData<ValidationResult> validationError = new MutableLiveData<>();
    private final MutableLiveData<RegisterParams> registerTrigger = new MutableLiveData<>();

    private final LiveData<NetworkResultWrapper<String>> registerResult;

    @Inject
    public RegisterViewModel(RegisterUseCase registerUseCase) {
        this.registerUseCase = registerUseCase;
        this.registerResult = Transformations.switchMap(
            registerTrigger,
            params -> this.registerUseCase.execute(
                params.name,
                params.email,
                params.phone,
                params.password,
                params.role
            )
        );
    }

    public LiveData<UserRole> getSelectedRole() {
        return selectedRole;
    }

    public LiveData<ValidationResult> getValidationError() {
        return validationError;
    }

    public LiveData<NetworkResultWrapper<String>> getRegisterResult() {
        return registerResult;
    }

    public void selectRole(UserRole role) {
        selectedRole.setValue(role);
    }

    public void register(String name, String email, String phone, String password) {
        UserRole role = selectedRole.getValue();

        ValidationResult validation = registerUseCase.validate(name, email, phone, password, role);
        if (!validation.isValid()) {
            validationError.setValue(validation);
            return;
        }

        validationError.setValue(ValidationResult.valid());
        registerTrigger.setValue(new RegisterParams(name, email, phone, password, role));
    }

    private static class RegisterParams {
        final String name;
        final String email;
        final String phone;
        final String password;
        final UserRole role;
        RegisterParams(String name, String email, String phone, String password, UserRole role) {
            this.name = name;
            this.email = email;
            this.phone = phone;
            this.password = password;
            this.role = role;
        }
    }
}
