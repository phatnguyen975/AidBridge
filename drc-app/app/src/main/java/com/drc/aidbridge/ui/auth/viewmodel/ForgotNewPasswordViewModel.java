package com.drc.aidbridge.ui.auth.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.Transformations;

import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.domain.usecase.auth.ResetPasswordUseCase;
import com.drc.aidbridge.domain.usecase.validation.ValidationResult;
import com.drc.aidbridge.ui.base.BaseViewModel;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

/**
 * ForgotNewPasswordViewModel — Step 3 of the forgot-password flow.
 */
@HiltViewModel
public class ForgotNewPasswordViewModel extends BaseViewModel {

    private final ResetPasswordUseCase resetPasswordUseCase;
    private final SavedStateHandle savedStateHandle;

    private final MutableLiveData<ValidationResult> validationError = new MutableLiveData<>();
    private final MutableLiveData<ChangePasswordParams> changePasswordTrigger = new MutableLiveData<>();
    private final LiveData<NetworkResultWrapper<String>> changePasswordResult;

    @Inject
    public ForgotNewPasswordViewModel(ResetPasswordUseCase resetPasswordUseCase,
                                      SavedStateHandle savedStateHandle) {
        this.resetPasswordUseCase = resetPasswordUseCase;
        this.savedStateHandle = savedStateHandle;

        this.changePasswordResult = Transformations.switchMap(
            changePasswordTrigger,
            params -> this.resetPasswordUseCase.execute(params.email, params.newPassword)
        );
    }

    public LiveData<ValidationResult> getValidationError() {
        return validationError;
    }

    public LiveData<NetworkResultWrapper<String>> getChangePasswordResult() {
        return changePasswordResult;
    }

    public String getEmail() {
        String email = savedStateHandle.get("email");
        return email != null ? email : "";
    }


    public void changePassword(String newPassword, String confirmPassword) {
        String email = getEmail();
        ValidationResult validation = resetPasswordUseCase.validate(email, newPassword, confirmPassword);

        if (!validation.isValid()) {
            validationError.setValue(validation);
            return;
        }

        validationError.setValue(ValidationResult.valid());
        changePasswordTrigger.setValue(new ChangePasswordParams(email, newPassword));
    }

    private static class ChangePasswordParams {
        final String email;
        final String newPassword;
        ChangePasswordParams(String email, String newPassword) {
            this.email = email;
            this.newPassword = newPassword;
        }
    }
}
