package com.drc.aidbridge.data.remote.api;

import com.drc.aidbridge.data.remote.dto.response.AuthResponse;
import com.drc.aidbridge.data.remote.dto.request.ForgotPasswordRequest;
import com.drc.aidbridge.data.remote.dto.request.LoginRequest;
import com.drc.aidbridge.data.remote.dto.request.OtpVerifyRequest;
import com.drc.aidbridge.data.remote.dto.request.RegisterRequest;
import com.drc.aidbridge.data.remote.dto.request.ResetPasswordRequest;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

/**
 * AuthApiService — Retrofit interface for all unauthenticated auth endpoints.
 *
 * These calls do NOT require an Authorization header (public endpoints).
 * The AuthInterceptor is designed to skip the Bearer token injection for /auth/* paths.
 */
public interface AuthApiService {

    /**
     * Authenticates the user with email + password.
     * Returns access + refresh tokens and user profile on success.
     */
    @POST("auth/login")
    Call<AuthResponse> login(@Body LoginRequest request);

    /**
     * Registers a new user account.
     * On success, triggers OTP sending to the user's email/phone and returns initial tokens.
     */
    @POST("auth/register")
    Call<AuthResponse> register(@Body RegisterRequest request);

    /**
     * Verifies the OTP submitted by the user after registration or sensitive operations.
     * Returns updated tokens on success.
     */
    @POST("auth/verify-otp")
    Call<AuthResponse> verifyOtp(@Body OtpVerifyRequest request);

    /**
     * Resends an OTP to the user's email/phone.
     */
    @POST("auth/resend-otp")
    Call<Void> resendOtp(@Body ForgotPasswordRequest request);

    @POST("auth/forgot-password")
    Call<Void> forgotPassword(@Body ForgotPasswordRequest request);

    @POST("auth/verify-reset-otp")
    Call<Void> verifyResetOtp(@Body OtpVerifyRequest request);

    @POST("auth/reset-password")
    Call<Void> resetPassword(@Body ResetPasswordRequest request);

    /**
     * Refreshes the access token using the long-lived refresh token.
     * Called automatically by TokenRefreshInterceptor when a 401 is received.
     */
    @POST("auth/refresh-token")
    Call<AuthResponse> refreshToken(@Body java.util.Map<String, String> body);
}
