package com.drc.aidbridge.ui.auth.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.Transformations;

import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.domain.usecase.auth.ResetPasswordUseCase;
import com.drc.aidbridge.domain.usecase.validation.AuthValidationResult;
import com.drc.aidbridge.ui.base.BaseViewModel;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

/**
 * ForgotNewPasswordViewModel â€” Step 3 of the forgot-password flow.
 */
@HiltViewModel
public class ForgotNewPasswordViewModel extends BaseViewModel {

    private final ResetPasswordUseCase resetPasswordUseCase;
    private final SavedStateHandle savedStateHandle;

    private final MutableLiveData<AuthValidationResult> validationError = new MutableLiveData<>();
    private final MutableLiveData<ChangePasswordParams> changePasswordTrigger = new MutableLiveData<>();
    private final LiveData<NetworkResultWrapper<String>> changePasswordResult;

    @Inject
    public ForgotNewPasswordViewModel(ResetPasswordUseCase resetPasswordUseCase,
                                      SavedStateHandle savedStateHandle) {
        this.resetPasswordUseCase = resetPasswordUseCase;
        this.savedStateHandle = savedStateHandle;

        this.changePasswordResult = Transformations.switchMap(
            changePasswordTrigger,
            params -> this.resetPasswordUseCase.execute(params.email, params.otp, params.newPassword)
        );
    }

    public LiveData<AuthValidationResult> getValidationError() {
        return validationError;
    }

    public LiveData<NetworkResultWrapper<String>> getChangePasswordResult() {
        return changePasswordResult;
    }

    public String getEmail() {
        String email = savedStateHandle.get("email");
        return email != null ? email : "";
    }

    public String getOtp() {
        String otp = savedStateHandle.get("otp");
        return otp != null ? otp : "";
    }


    public void changePassword(String newPassword, String confirmPassword) {
        String email = getEmail();
        String otp = getOtp();
        AuthValidationResult validation = resetPasswordUseCase.validate(email, otp, newPassword, confirmPassword);

        if (!validation.isValid()) {
            validationError.setValue(validation);
            return;
        }

        validationError.setValue(AuthValidationResult.valid());
        changePasswordTrigger.setValue(new ChangePasswordParams(email, otp, newPassword));
    }

    private static class ChangePasswordParams {
        final String email;
        final String otp;
        final String newPassword;
        ChangePasswordParams(String email, String otp, String newPassword) {
            this.email = email;
            this.otp = otp;
            this.newPassword = newPassword;
        }
    }
}

