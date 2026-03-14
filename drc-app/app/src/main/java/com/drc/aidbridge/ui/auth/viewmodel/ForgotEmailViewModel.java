package com.drc.aidbridge.ui.auth.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.domain.usecase.auth.RequestResetOtpUseCase;
import com.drc.aidbridge.domain.usecase.common.validation.ValidationResult;
import com.drc.aidbridge.ui.base.BaseViewModel;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

/**
 * ForgotEmailViewModel — Step 1 of the forgot-password flow.
 *
 * Responsibilities:
 * - Accepts a user-supplied email address.
 * - Requests the server to send a password-reset OTP to that email.
 * - Exposes sendOtpResult as a typed NetworkResultWrapper LiveData stream.
 *
 * Triển khai hiện tại: gọi API thật qua AuthRepository.
 *   Endpoint: POST /auth/forgot-password  { "email": "<email>" }
 *   Response: 200 OK | 404 email not found
 */
@HiltViewModel
public class ForgotEmailViewModel extends BaseViewModel {

    private final RequestResetOtpUseCase requestResetOtpUseCase;

    /**
     * Emits Loading while the request is in flight, then Success (ignored string) or Error
     * (user-facing message to display).
     */
    private final MutableLiveData<NetworkResultWrapper<String>> sendOtpResult =
            new MutableLiveData<>();

    @Inject
    public ForgotEmailViewModel(RequestResetOtpUseCase requestResetOtpUseCase) {
        this.requestResetOtpUseCase = requestResetOtpUseCase;
    }

    /** @return LiveData stream representing the send-OTP operation state. */
    public LiveData<NetworkResultWrapper<String>> getSendOtpResult() {
        return sendOtpResult;
    }

    /**
     * Sends a password-reset OTP to the given email address.
     *
     * @param email The user's registered email address (already validated by the Fragment).
     */
    public void sendOtp(String email) {
        ValidationResult validation = requestResetOtpUseCase.validate(email);
        if (!validation.isValid()) {
            sendOtpResult.setValue(NetworkResultWrapper.error(validation.getErrorMessage()));
            return;
        }

        sendOtpResult.setValue(NetworkResultWrapper.loading());

        LiveData<NetworkResultWrapper<String>> source = requestResetOtpUseCase.execute(email);
        source.observeForever(new Observer<NetworkResultWrapper<String>>() {
            @Override
            public void onChanged(NetworkResultWrapper<String> result) {
                if (result == null) {
                    return;
                }
                sendOtpResult.postValue(result);
                if (!(result instanceof NetworkResultWrapper.Loading)) {
                    source.removeObserver(this);
                }
            }
        });
    }
}
