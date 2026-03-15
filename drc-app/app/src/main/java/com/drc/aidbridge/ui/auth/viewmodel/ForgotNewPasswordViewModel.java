package com.drc.aidbridge.ui.auth.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.SavedStateHandle;

import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.domain.usecase.auth.ResetPasswordUseCase;
import com.drc.aidbridge.domain.usecase.validation.ValidationResult;
import com.drc.aidbridge.ui.base.BaseViewModel;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

/**
 * ForgotNewPasswordViewModel — Step 3 of the forgot-password flow.
 *
 * Responsibilities:
 * - Holds the email forwarded from ForgotOtpFragment (via Navigation Safe Args /
 *   SavedStateHandle).
 * - Submits the new password to the server after the OTP has been verified.
 * - Exposes changePasswordResult as a typed NetworkResultWrapper LiveData stream.
 *
 * Triển khai hiện tại: gọi API thật qua AuthRepository.
 *   Endpoint: POST /auth/reset-password  { "email": "...", "newPassword": "..." }
 *   Response: 200 OK | 400 error
 */
@HiltViewModel
public class ForgotNewPasswordViewModel extends BaseViewModel {

    private final ResetPasswordUseCase resetPasswordUseCase;
    private final SavedStateHandle savedStateHandle;

    /**
     * Emits Loading while the request is in flight, then Success (message) or Error
     * (user-facing error message).
     */
    private final MutableLiveData<NetworkResultWrapper<String>> changePasswordResult =
            new MutableLiveData<>();

    @Inject
    public ForgotNewPasswordViewModel(ResetPasswordUseCase resetPasswordUseCase,
                                      SavedStateHandle savedStateHandle) {
        this.resetPasswordUseCase = resetPasswordUseCase;
        this.savedStateHandle = savedStateHandle;
    }

    /** @return LiveData stream representing the change-password operation state. */
    public LiveData<NetworkResultWrapper<String>> getChangePasswordResult() {
        return changePasswordResult;
    }

    /**
     * Returns the email address passed from the previous screen via Navigation arguments.
     * Safe to call before changePassword() — never null.
     */
    public String getEmail() {
        String email = savedStateHandle.get("email");
        return email != null ? email : "";
    }

    /**
     * Submits the new password for the account identified by {@link #getEmail()}.
     *
     * @param otpCode The OTP code received by the user.
     * @param newPassword The validated new password (length and match already checked by the
     *                    Fragment before calling this method).
     */
    public void changePassword(String newPassword) {
        ValidationResult validation =
                resetPasswordUseCase.validate(getEmail(), newPassword);
        if (!validation.isValid()) {
            changePasswordResult.setValue(NetworkResultWrapper.error(validation.getErrorMessage()));
            return;
        }

        changePasswordResult.setValue(NetworkResultWrapper.loading());

        LiveData<NetworkResultWrapper<String>> source =
                resetPasswordUseCase.execute(getEmail(), newPassword);
        source.observeForever(new Observer<NetworkResultWrapper<String>>() {
            @Override
            public void onChanged(NetworkResultWrapper<String> result) {
                if (result == null) {
                    return;
                }
                changePasswordResult.postValue(result);
                if (!(result instanceof NetworkResultWrapper.Loading)) {
                    source.removeObserver(this);
                }
            }
        });
    }
}
