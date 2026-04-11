package com.drc.aidbridge.ui.auth.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.domain.usecase.auth.RequestResetOtpUseCase;
import com.drc.aidbridge.domain.usecase.validation.AuthValidationResult;
import com.drc.aidbridge.ui.base.BaseViewModel;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

/**
 * ForgotEmailViewModel â€” Step 1 of the forgot-password flow.
 */
@HiltViewModel
public class ForgotEmailViewModel extends BaseViewModel {

    private final RequestResetOtpUseCase requestResetOtpUseCase;

    private final MutableLiveData<AuthValidationResult> validationError = new MutableLiveData<>();
    private final MutableLiveData<SendOtpParams> sendOtpTrigger = new MutableLiveData<>();
    private final LiveData<NetworkResultWrapper<String>> sendOtpResult;

    @Inject
    public ForgotEmailViewModel(RequestResetOtpUseCase requestResetOtpUseCase) {
        this.requestResetOtpUseCase = requestResetOtpUseCase;
        this.sendOtpResult = Transformations.switchMap(
            sendOtpTrigger,
            params -> this.requestResetOtpUseCase.execute(params.email)
        );
    }

    public LiveData<AuthValidationResult> getValidationError() {
        return validationError;
    }

    public LiveData<NetworkResultWrapper<String>> getSendOtpResult() {
        return sendOtpResult;
    }

    public void sendOtp(String email) {
        AuthValidationResult validation = requestResetOtpUseCase.validate(email);

        if (!validation.isValid()) {
            validationError.setValue(validation);
            return;
        }

        validationError.setValue(AuthValidationResult.valid());
        sendOtpTrigger.setValue(new SendOtpParams(email));
    }

    private static class SendOtpParams {
        final String email;
        SendOtpParams(String email) {
            this.email = email;
        }
    }
}

