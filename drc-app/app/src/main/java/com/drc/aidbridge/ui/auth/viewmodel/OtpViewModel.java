package com.drc.aidbridge.ui.auth.viewmodel;

import android.os.CountDownTimer;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.SavedStateHandle;

import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.domain.usecase.auth.ResendOtpUseCase;
import com.drc.aidbridge.domain.usecase.auth.VerifyOtpUseCase;
import com.drc.aidbridge.domain.usecase.common.validation.ValidationResult;
import com.drc.aidbridge.ui.base.BaseViewModel;
import com.drc.aidbridge.utils.Constants;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

/**
 * OtpViewModel — manages:
 * - Countdown timer (59 → 0)
 * - resendEnabled flag (true when countdown reaches 0)
 * - verifyResult LiveData (success / error)
 * - email argument received from RegisterFragment via SavedStateHandle
 */
@HiltViewModel
public class OtpViewModel extends BaseViewModel {

    private final ResendOtpUseCase resendOtpUseCase;
    private final VerifyOtpUseCase verifyOtpUseCase;
    private final SavedStateHandle savedStateHandle;

    /** Countdown seconds remaining */
    private final MutableLiveData<Integer> countdown =
            new MutableLiveData<>(Constants.OTP_COUNTDOWN_SEC);

    /** Whether the "Gửi lại" button is enabled */
    private final MutableLiveData<Boolean> resendEnabled = new MutableLiveData<>(false);

    /** Verify OTP result */
    private final MutableLiveData<NetworkResultWrapper<String>> verifyResult =
            new MutableLiveData<>();

        private final MutableLiveData<NetworkResultWrapper<Boolean>> resendResult =
            new MutableLiveData<>();

    private CountDownTimer countDownTimer;

    @Inject
    public OtpViewModel(ResendOtpUseCase resendOtpUseCase,
                        VerifyOtpUseCase verifyOtpUseCase,
                        SavedStateHandle savedStateHandle) {
        this.resendOtpUseCase = resendOtpUseCase;
        this.verifyOtpUseCase = verifyOtpUseCase;
        this.savedStateHandle = savedStateHandle;
        startCountdown();
    }

    // ------------------------------------------------------------------
    // Exposed LiveData
    // ------------------------------------------------------------------

    public LiveData<Integer> getCountdown() {
        return countdown;
    }

    public LiveData<Boolean> getResendEnabled() {
        return resendEnabled;
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

    // ------------------------------------------------------------------
    // OTP Verification
    // ------------------------------------------------------------------

    /**
    * Verifies the entered OTP bằng API thông qua VerifyOtpUseCase.
     */
    public void verify(String otp) {
        // Validate format (6 digits)
        ValidationResult validation = verifyOtpUseCase.validate(otp);
        if (!validation.isValid()) {
            verifyResult.setValue(NetworkResultWrapper.error(validation.getErrorMessage()));
            return;
        }

        verifyResult.setValue(NetworkResultWrapper.loading());

        LiveData<NetworkResultWrapper<String>> source =
                verifyOtpUseCase.execute(getEmail(), otp);

        // Self-removing observeForever: bridges source → verifyResult.
        // Disconnects once a terminal (Success / Error) state arrives.
        source.observeForever(new Observer<NetworkResultWrapper<String>>() {
            @Override
            public void onChanged(NetworkResultWrapper<String> result) {
                if (result == null) return;
                verifyResult.postValue(result);
                if (!(result instanceof NetworkResultWrapper.Loading)) {
                    source.removeObserver(this);
                }
            }
        });
    }

    // ------------------------------------------------------------------
    // Resend OTP
    // ------------------------------------------------------------------

    /**
    * Triggered when user taps "Gửi lại".
    * Resets countdown và gọi API gửi lại OTP.
     */
    public void resendOtp() {
        if (Boolean.TRUE.equals(resendEnabled.getValue())) {
            ValidationResult validation = resendOtpUseCase.validate(getEmail());
            if (!validation.isValid()) {
                resendResult.setValue(NetworkResultWrapper.error(validation.getErrorMessage()));
                return;
            }

            resendEnabled.setValue(false);
            startCountdown();

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

    // ------------------------------------------------------------------
    // Countdown
    // ------------------------------------------------------------------

    private void startCountdown() {
        cancelCountdown();
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
                resendEnabled.postValue(true);
            }
        };
        countDownTimer.start();
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
