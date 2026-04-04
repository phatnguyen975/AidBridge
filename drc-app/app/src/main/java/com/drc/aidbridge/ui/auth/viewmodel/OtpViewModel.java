package com.drc.aidbridge.ui.auth.viewmodel;

import android.os.CountDownTimer;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.Transformations;

import com.drc.aidbridge.R;
import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.data.remote.dto.response.AuthResponse;
import com.drc.aidbridge.domain.usecase.auth.ResendOtpUseCase;
import com.drc.aidbridge.domain.usecase.auth.VerifyOtpUseCase;
import com.drc.aidbridge.domain.usecase.validation.ValidationResult;
import com.drc.aidbridge.ui.base.BaseViewModel;
import com.drc.aidbridge.utils.Constants;
import com.drc.aidbridge.utils.TokenManager;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

/**
 * OtpViewModel — manages the OTP verification in the registration flow.
 */
@HiltViewModel
public class OtpViewModel extends BaseViewModel {

    public enum ResendUiState {
        TIMER_RUNNING,
        READY,
        LOADING
    }

    private final ResendOtpUseCase resendOtpUseCase;
    private final VerifyOtpUseCase verifyOtpUseCase;
    private final TokenManager tokenManager;
    private final SavedStateHandle savedStateHandle;

    private final MutableLiveData<Integer> countdown =
            new MutableLiveData<>(Constants.OTP_COUNTDOWN_SEC);
    private final MutableLiveData<ResendUiState> resendUiState =
            new MutableLiveData<>(ResendUiState.TIMER_RUNNING);

    private final MutableLiveData<ValidationResult> validationError = new MutableLiveData<>();
    private final MutableLiveData<Integer> otpInlineErrorResId = new MutableLiveData<>();
    private final MutableLiveData<VerifyParams> verifyTrigger = new MutableLiveData<>();
    private final MutableLiveData<ResendParams> resendTrigger = new MutableLiveData<>();

    private final MediatorLiveData<NetworkResultWrapper<AuthResponse>> verifyResult =
            new MediatorLiveData<>();
    private final MediatorLiveData<NetworkResultWrapper<Boolean>> resendResult =
            new MediatorLiveData<>();

    private CountDownTimer countDownTimer;

    @Inject
    public OtpViewModel(ResendOtpUseCase resendOtpUseCase,
                        VerifyOtpUseCase verifyOtpUseCase,
                        TokenManager tokenManager,
                        SavedStateHandle savedStateHandle) {
        this.resendOtpUseCase = resendOtpUseCase;
        this.verifyOtpUseCase = verifyOtpUseCase;
        this.tokenManager = tokenManager;
        this.savedStateHandle = savedStateHandle;

        LiveData<NetworkResultWrapper<AuthResponse>> verifySource = Transformations.switchMap(
            verifyTrigger,
            params -> this.verifyOtpUseCase.execute(params.email, params.otp)
        );

        LiveData<NetworkResultWrapper<Boolean>> resendSource = Transformations.switchMap(
            resendTrigger,
            params -> this.resendOtpUseCase.execute(params.email)
        );

        verifyResult.addSource(verifySource, result -> {
            verifyResult.setValue(result);
            if (result != null && result.isSuccess()) {
                AuthResponse payload = result.getData();
                persistVerifiedSession(payload);
            }
        });

        resendResult.addSource(resendSource, result -> {
            resendResult.setValue(result);
            updateResendStateFromApi(result);
        });

        startCountdown();
    }

    public LiveData<Integer> getCountdown() {
        return countdown;
    }

    public LiveData<ResendUiState> getResendUiState() {
        return resendUiState;
    }

    public LiveData<NetworkResultWrapper<AuthResponse>> getVerifyResult() {
        return verifyResult;
    }

    public LiveData<Integer> getOtpInlineErrorResId() {
        return otpInlineErrorResId;
    }

    public LiveData<ValidationResult> getValidationError() {
        return validationError;
    }

    public LiveData<NetworkResultWrapper<Boolean>> getResendResult() {
        return resendResult;
    }

    public String getEmail() {
        String email = savedStateHandle.get("email");
        return email != null ? email : "";
    }

    public void verify(String otp) {
        String normalizedOtp = otp != null ? otp.trim() : "";
        if (normalizedOtp.length() < 6) {
            otpInlineErrorResId.setValue(R.string.otp_error_required_6_digits);
            return;
        }

        ValidationResult validation = verifyOtpUseCase.validate(normalizedOtp);
        if (!validation.isValid()) {
            otpInlineErrorResId.setValue(R.string.otp_error_required_6_digits);
            return;
        }

        clearOtpInlineError();
        verifyTrigger.setValue(new VerifyParams(getEmail(), normalizedOtp));
    }

    public void resendOtp() {
        if (resendUiState.getValue() != ResendUiState.READY) {
            return;
        }

        String email = getEmail();
        ValidationResult validation = resendOtpUseCase.validate(email);
        if (!validation.isValid()) {
            validationError.setValue(validation);
            return;
        }

        validationError.setValue(ValidationResult.valid());
        resendUiState.setValue(ResendUiState.LOADING);
        resendTrigger.setValue(new ResendParams(email));
    }

    public void clearOtpInlineError() {
        otpInlineErrorResId.setValue(null);
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

    private void startCountdown() {
        cancelCountdown();
        countdown.setValue(Constants.OTP_COUNTDOWN_SEC);
        resendUiState.setValue(ResendUiState.TIMER_RUNNING);

        countDownTimer = new CountDownTimer(
                Constants.OTP_COUNTDOWN_SEC * 1000L,
                1000L
        ) {
            @Override
            public void onTick(long millisUntilFinished) {
                countdown.postValue((int) (millisUntilFinished / 1000));
            }

            @Override
            public void onFinish() {
                countdown.postValue(0);
                resendUiState.postValue(ResendUiState.READY);
            }
        };
        countDownTimer.start();
    }

    private void updateResendStateFromApi(NetworkResultWrapper<Boolean> result) {
        if (result == null) {
            return;
        }

        if (result.isLoading()) {
            resendUiState.postValue(ResendUiState.LOADING);
            return;
        }

        if (result.isSuccess()) {
            startCountdown();
            return;
        }

        if (result.isError()) {
            resendUiState.postValue(ResendUiState.READY);
        }
    }

    private void persistVerifiedSession(AuthResponse authResponse) {
        if (authResponse == null) {
            return;
        }

        tokenManager.saveTokens(authResponse.getAccessToken(), authResponse.getRefreshToken());
        tokenManager.markUserAsVerified(true);
    }

    private void cancelCountdown() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        cancelCountdown();
    }
}
