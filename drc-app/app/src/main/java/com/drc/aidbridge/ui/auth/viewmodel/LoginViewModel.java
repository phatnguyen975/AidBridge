package com.drc.aidbridge.ui.auth.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.domain.model.User;
import com.drc.aidbridge.domain.usecase.auth.LoginUseCase;
import com.drc.aidbridge.domain.usecase.validation.ValidationResult;
import com.drc.aidbridge.ui.base.BaseViewModel;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class LoginViewModel extends BaseViewModel {

    private final LoginUseCase loginUseCase;

    private final MutableLiveData<ValidationResult> validationError = new MutableLiveData<>();
    private final MutableLiveData<LoginParams> loginTrigger = new MutableLiveData<>();

    private final LiveData<NetworkResultWrapper<User>> loginResult;

    @Inject
    public LoginViewModel(LoginUseCase loginUseCase) {
        this.loginUseCase = loginUseCase;
        this.loginResult = Transformations.switchMap(
            loginTrigger,
            params -> this.loginUseCase.execute(params.email, params.password)
        );
    }

    public LiveData<ValidationResult> getValidationError() {
        return validationError;
    }

    public LiveData<NetworkResultWrapper<User>> getLoginResult() {
        return loginResult;
    }

    public void login(String email, String password) {
        ValidationResult validation = loginUseCase.validate(email, password);

        if (!validation.isValid()) {
            validationError.setValue(validation);
            return;
        }

        validationError.setValue(ValidationResult.valid());
        loginTrigger.setValue(new LoginParams(email, password));
    }

    private static class LoginParams {
        String email;
        String password;
        LoginParams(String email, String password) {
            this.email = email;
            this.password = password;
        }
    }
}
