package com.drc.aidbridge.ui.auth.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.SavedStateHandle;

import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.domain.usecase.auth.VerifyResetOtpUseCase;
import com.drc.aidbridge.domain.usecase.auth.ResendOtpUseCase;
import com.drc.aidbridge.domain.usecase.common.validation.ValidationResult;
import com.drc.aidbridge.ui.base.BaseViewModel;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

/**
 * ForgotOtpViewModel — Step 2 of the forgot-password flow.
 *
 * Responsibilities:
 * - Holds the email forwarded from ForgotEmailFragment (via Navigation Safe Args /
 *   SavedStateHandle).
 * - Validates and submits the 6-digit OTP entered by the user.
 * - Exposes verifyResult as a typed NetworkResultWrapper LiveData stream.
 *
 * Triển khai hiện tại: gọi API thật qua AuthRepository.
 *   Endpoint: POST /auth/verify-reset-otp  { "email": "...", "otpCode": "..." }
 *   Response: 200 OK | 400 invalid OTP
 */
@HiltViewModel
public class ForgotOtpViewModel extends BaseViewModel {

    private final VerifyResetOtpUseCase verifyResetOtpUseCase;
    private final ResendOtpUseCase resendOtpUseCase;
    private final SavedStateHandle savedStateHandle;

    /**
     * Emits Loading while the request is in flight, then Success (message) or Error
     * (user-facing error message).
     */
    private final MutableLiveData<NetworkResultWrapper<String>> verifyResult =
            new MutableLiveData<>();

    private final MutableLiveData<NetworkResultWrapper<Boolean>> resendResult =
            new MutableLiveData<>();

    @Inject
    public ForgotOtpViewModel(VerifyResetOtpUseCase verifyResetOtpUseCase,
                              ResendOtpUseCase resendOtpUseCase,
                              SavedStateHandle savedStateHandle) {
        this.verifyResetOtpUseCase = verifyResetOtpUseCase;
        this.resendOtpUseCase = resendOtpUseCase;
        this.savedStateHandle = savedStateHandle;
    }

    /** @return LiveData stream representing the OTP verification operation state. */
    public LiveData<NetworkResultWrapper<String>> getVerifyResult() {
        return verifyResult;
    }

    public LiveData<NetworkResultWrapper<Boolean>> getResendResult() {
        return resendResult;
    }

    /**
     * Returns the email address passed from the previous screen via Navigation arguments.
     * Safe to call before verify() — never null.
     */
    public String getEmail() {
        String email = savedStateHandle.get("email");
        return email != null ? email : "";
    }

    /**
     * Validates and verifies the entered OTP.
     *
     * Posts an immediate Error if the OTP is not exactly 6 digits.
     * Posts Loading while the async call is in flight.
    * Posts Success hoặc Error theo phản hồi API từ server.
     *
     * @param otp The 6-character string collected from the OTP boxes.
     */
    public void verify(String otp) {
        ValidationResult validation =
                verifyResetOtpUseCase.validate(getEmail(), otp);
        if (!validation.isValid()) {
            verifyResult.setValue(NetworkResultWrapper.error(validation.getErrorMessage()));
            return;
        }

        verifyResult.setValue(NetworkResultWrapper.loading());

        LiveData<NetworkResultWrapper<String>> source =
                verifyResetOtpUseCase.execute(getEmail(), otp);
        source.observeForever(new Observer<NetworkResultWrapper<String>>() {
            @Override
            public void onChanged(NetworkResultWrapper<String> result) {
                if (result == null) {
                    return;
                }
                verifyResult.postValue(result);
                if (!(result instanceof NetworkResultWrapper.Loading)) {
                    source.removeObserver(this);
                }
            }
        });
    }

    public void resendOtp() {
        ValidationResult validation = resendOtpUseCase.validate(getEmail());
        if (!validation.isValid()) {
            resendResult.setValue(NetworkResultWrapper.error(validation.getErrorMessage()));
            return;
        }

        resendResult.setValue(NetworkResultWrapper.loading());

        LiveData<NetworkResultWrapper<Boolean>> source = resendOtpUseCase.execute(getEmail());
        source.observeForever(new Observer<NetworkResultWrapper<Boolean>>() {
            @Override
            public void onChanged(NetworkResultWrapper<Boolean> result) {
                if (result == null) {
                    return;
                }
                resendResult.postValue(result);
                if (!(result instanceof NetworkResultWrapper.Loading)) {
                    source.removeObserver(this);
                }
            }
        });
    }
}
