package com.drc.aidbridge.domain.repository;

import androidx.lifecycle.LiveData;
import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.data.remote.dto.response.AuthResponse;
import com.drc.aidbridge.data.remote.dto.request.LoginRequest;
import com.drc.aidbridge.data.remote.dto.request.RegisterRequest;
import com.drc.aidbridge.data.remote.dto.request.OtpVerifyRequest;
import com.drc.aidbridge.data.remote.dto.request.ForgotPasswordRequest;
import com.drc.aidbridge.data.remote.dto.request.ResetPasswordRequest;
import com.drc.aidbridge.domain.model.User;

/**
 * AuthRepository — interface defining the contract for all authentication operations.
 */
public interface AuthRepository {

    /**
     * Authenticates a user with email and password.
     * Returns a LiveData stream of NetworkResultWrapper<User> that emits:
     * - Loading: while the API call is in flight
     * - Success: on successful login, with the authenticated User domain model
     * - Error:   on failure (wrong credentials, network error, etc.)
     */
    LiveData<NetworkResultWrapper<User>> login(LoginRequest request);

    /**
     * Registers a new user account.
     * Returns a stream emitting Loading → Success/Error.
     */
    LiveData<NetworkResultWrapper<User>> register(RegisterRequest request);

    /**
     * Verifies a 6-digit OTP code for the given email.
      * On success, returns the auth payload including refreshed tokens and user info.
     */
     LiveData<NetworkResultWrapper<AuthResponse>> verifyOtp(OtpVerifyRequest request);

    /**
     * Requests a password-reset OTP for the given email.
     */
    LiveData<NetworkResultWrapper<String>> forgotPassword(ForgotPasswordRequest request);

    /**
     * Resends the OTP to the user's email/phone.
     * Success emits Boolean.TRUE; Error contains the failure message.
     */
    LiveData<NetworkResultWrapper<Boolean>> resendOtp(ForgotPasswordRequest request);

    /**
     * Verifies reset-password OTP code.
     */
    LiveData<NetworkResultWrapper<String>> verifyResetOtp(OtpVerifyRequest request);

    /**
     * Resets password after successful OTP verification.
     */
    LiveData<NetworkResultWrapper<String>> resetPassword(ResetPasswordRequest request);

    /**
     * Logs out the current user session.
     * Always clears local auth state regardless of API outcome.
     */
    LiveData<NetworkResultWrapper<Boolean>> logout(String refreshToken);

    /**
     * Updates the backend mapping for current device FCM token.
     */
    void updateFcmToken(String deviceId, String fcmToken);
}
