package com.drc.aidbridge.domain.usecase.auth;

import androidx.lifecycle.LiveData;

import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.data.remote.dto.request.OtpVerifyRequest;
import com.drc.aidbridge.domain.repository.AuthRepository;
import com.drc.aidbridge.domain.usecase.common.validation.AuthInputValidator;
import com.drc.aidbridge.domain.usecase.common.validation.ValidationResult;

import javax.inject.Inject;

/**
 * VerifyOtpUseCase — validates OTP format and delegates to AuthRepository.
 *
 * Split into validate() + execute() to match ViewModel pattern.
 */
public class VerifyOtpUseCase {

    private final AuthRepository authRepository;
    private final AuthInputValidator inputValidator;

    @Inject
    public VerifyOtpUseCase(AuthRepository authRepository, AuthInputValidator inputValidator) {
        this.authRepository = authRepository;
        this.inputValidator = inputValidator;
    }

    // ------------------------------------------------------------------
    // Validation
    // ------------------------------------------------------------------

    /** Validates that otp is exactly 6 numeric digits. */
    public ValidationResult validate(String otp) {
        return inputValidator.requireOtp(otp);
    }

    // ------------------------------------------------------------------
    // Execution
    // ------------------------------------------------------------------

    /**
     * Delegates to repository. Returns LiveData<NetworkResultWrapper<String>>
     * where String is the email on success.
     */
    public LiveData<NetworkResultWrapper<String>> execute(String email, String otpCode) {
        return authRepository.verifyOtp(
                new OtpVerifyRequest(inputValidator.normalizeEmail(email), otpCode));
    }
}

