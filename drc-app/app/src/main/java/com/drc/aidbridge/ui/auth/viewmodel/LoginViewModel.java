package com.drc.aidbridge.ui.auth.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.domain.model.User;
import com.drc.aidbridge.domain.usecase.auth.LoginUseCase;
import com.drc.aidbridge.domain.usecase.common.validation.ValidationResult;
import com.drc.aidbridge.ui.base.BaseViewModel;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class LoginViewModel extends BaseViewModel {

    private final MutableLiveData<NetworkResultWrapper<User>> loginResult = new MutableLiveData<>();

    private final LoginUseCase loginUseCase;

    @Inject
    public LoginViewModel(LoginUseCase loginUseCase) {
        this.loginUseCase = loginUseCase;
    }

    public LiveData<NetworkResultWrapper<User>> getLoginResult() {
        return loginResult;
    }

    public void login(String email, String password) {
        ValidationResult validation = loginUseCase.validate(email, password);
        if (!validation.isValid()) {
            loginResult.setValue(NetworkResultWrapper.error(validation.getErrorMessage()));
            return;
        }

        loginResult.setValue(NetworkResultWrapper.loading());

        LiveData<NetworkResultWrapper<User>> source = loginUseCase.execute(email, password);

        // Use a self-removing observeForever observer to bridge source → loginResult.
        // We observe source forever (no lifecycle owner) so it fires even when
        // the Fragment's view lifecycle is in a transient state.
        // The observer removes itself once a terminal state (Success / Error) arrives.
        source.observeForever(new Observer<NetworkResultWrapper<User>>() {
            @Override
            public void onChanged(NetworkResultWrapper<User> result) {
                if (result == null) return;
                loginResult.postValue(result);
                if (!(result instanceof NetworkResultWrapper.Loading)) {
                    source.removeObserver(this); // clean up after terminal state
                }
            }
        });
    }
}
