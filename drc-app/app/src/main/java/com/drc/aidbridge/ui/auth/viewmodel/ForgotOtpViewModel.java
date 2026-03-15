package com.drc.aidbridge.ui.auth.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.Transformations;

import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.domain.usecase.auth.VerifyResetOtpUseCase;
import com.drc.aidbridge.domain.usecase.auth.ResendOtpUseCase;
import com.drc.aidbridge.domain.usecase.validation.ValidationResult;
import com.drc.aidbridge.ui.base.BaseViewModel;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

/**
 * ForgotOtpViewModel — Step 2 of the forgot-password flow.
 */
@HiltViewModel
public class ForgotOtpViewModel extends BaseViewModel {

    private final VerifyResetOtpUseCase verifyResetOtpUseCase;
    private final ResendOtpUseCase resendOtpUseCase;
    private final SavedStateHandle savedStateHandle;

    private final MutableLiveData<ValidationResult> validationError = new MutableLiveData<>();
    private final MutableLiveData<VerifyParams> verifyTrigger = new MutableLiveData<>();
    private final MutableLiveData<ResendParams> resendTrigger = new MutableLiveData<>();

    private final LiveData<NetworkResultWrapper<String>> verifyResult;
    private final LiveData<NetworkResultWrapper<Boolean>> resendResult;

    @Inject
    public ForgotOtpViewModel(VerifyResetOtpUseCase verifyResetOtpUseCase,
                              ResendOtpUseCase resendOtpUseCase,
                              SavedStateHandle savedStateHandle) {
        this.verifyResetOtpUseCase = verifyResetOtpUseCase;
        this.resendOtpUseCase = resendOtpUseCase;
        this.savedStateHandle = savedStateHandle;

        this.verifyResult = Transformations.switchMap(
            verifyTrigger,
            params -> this.verifyResetOtpUseCase.execute(params.email, params.otp)
        );
        this.resendResult = Transformations.switchMap(
            resendTrigger,
            params -> this.resendOtpUseCase.execute(params.email)
        );
    }

    public LiveData<ValidationResult> getValidationError() {
        return validationError;
    }

    public LiveData<NetworkResultWrapper<String>> getVerifyResult() {
        return verifyResult;
    }

    public LiveData<NetworkResultWrapper<Boolean>> getResendResult() {
        return resendResult;
    }

    public String getEmail() {
        String email = savedStateHandle.get("email");
        return email != null ? email : "";
    }

    public void verify(String otp) {
        String email = getEmail();
        ValidationResult validation = verifyResetOtpUseCase.validate(email, otp);

        if (!validation.isValid()) {
            validationError.setValue(validation);
            return;
        }

        validationError.setValue(ValidationResult.valid());
        verifyTrigger.setValue(new VerifyParams(email, otp));
    }

    public void resendOtp() {
        String email = getEmail();
        ValidationResult validation = resendOtpUseCase.validate(email);

        if (!validation.isValid()) {
            validationError.setValue(validation);
            return;
        }

        validationError.setValue(ValidationResult.valid());
        resendTrigger.setValue(new ResendParams(email));
    }

    private static class VerifyParams {
        final String email;
        final String otp;
        VerifyParams(String email, String otp) {
            this.email = email;
            this.otp = otp;
        }
    }

    private static class ResendParams {
        final String email;
        ResendParams(String email) {
            this.email = email;
        }
    }
}
