package com.drc.aidbridge.ui.auth.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.domain.enums.UserRole;
import com.drc.aidbridge.domain.usecase.auth.RegisterUseCase;
import com.drc.aidbridge.domain.usecase.common.validation.ValidationResult;
import com.drc.aidbridge.ui.base.BaseViewModel;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class RegisterViewModel extends BaseViewModel {

    private final RegisterUseCase registerUseCase;

    private final MutableLiveData<UserRole> selectedRole = new MutableLiveData<>(null);

    private final MutableLiveData<NetworkResultWrapper<String>> registerResult =
            new MutableLiveData<>();

    @Inject
    public RegisterViewModel(RegisterUseCase registerUseCase) {
        this.registerUseCase = registerUseCase;
    }

    public LiveData<UserRole> getSelectedRole() {
        return selectedRole;
    }

    public LiveData<NetworkResultWrapper<String>> getRegisterResult() {
        return registerResult;
    }

    public void selectRole(UserRole role) {
        selectedRole.setValue(role);
    }

    public void register(String name, String email, String phone, String password) {
        UserRole role = selectedRole.getValue();

        // Validate inputs via use case — returns error immediately if invalid
        ValidationResult validation =
                registerUseCase.validate(name, email, phone, password, role);
        if (!validation.isValid()) {
            registerResult.setValue(NetworkResultWrapper.error(validation.getErrorMessage()));
            return;
        }

        registerResult.setValue(NetworkResultWrapper.loading());

        LiveData<NetworkResultWrapper<String>> source =
                registerUseCase.execute(name, email, phone, password, role);

        // Self-removing observeForever: bridges source → registerResult.
        // Disconnects once a terminal (Success / Error) state arrives.
        source.observeForever(new Observer<NetworkResultWrapper<String>>() {
            @Override
            public void onChanged(NetworkResultWrapper<String> result) {
                if (result == null) return;
                registerResult.postValue(result);
                if (!(result instanceof NetworkResultWrapper.Loading)) {
                    source.removeObserver(this);
                }
            }
        });
    }
}

