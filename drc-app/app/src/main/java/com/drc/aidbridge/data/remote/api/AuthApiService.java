package com.drc.aidbridge.data.remote.api;

import com.drc.aidbridge.data.remote.dto.response.AuthResponse;
import com.drc.aidbridge.data.remote.dto.response.BaseResponse;
import com.drc.aidbridge.data.remote.dto.request.ForgotPasswordRequest;
import com.drc.aidbridge.data.remote.dto.request.LoginRequest;
import com.drc.aidbridge.data.remote.dto.request.LogoutRequest;
import com.drc.aidbridge.data.remote.dto.request.OtpVerifyRequest;
import com.drc.aidbridge.data.remote.dto.request.RefreshTokenRequest;
import com.drc.aidbridge.data.remote.dto.request.RegisterRequest;
import com.drc.aidbridge.data.remote.dto.request.ResetPasswordRequest;
import com.drc.aidbridge.data.remote.dto.request.UpdateFcmTokenRequest;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;

/**
 * AuthApiService — Retrofit interface for all unauthenticated auth endpoints.
 *
 * These calls do NOT require an Authorization header (public endpoints).
 * The AuthInterceptor is designed to skip the Bearer token injection for /auth/* paths.
 */
public interface AuthApiService {

    @POST("auth/login")
    Call<BaseResponse<AuthResponse>> login(@Body LoginRequest request);

    @POST("auth/register")
    Call<BaseResponse<AuthResponse>> register(@Body RegisterRequest request);

    @POST("auth/verify-otp")
    Call<BaseResponse<AuthResponse>> verifyOtp(@Body OtpVerifyRequest request);

    @POST("auth/resend-otp")
    Call<BaseResponse<String>> resendOtp(@Body ForgotPasswordRequest request);

    @POST("auth/forgot-password")
    Call<BaseResponse<String>> forgotPassword(@Body ForgotPasswordRequest request);

    @POST("auth/verify-otp")
    Call<BaseResponse<AuthResponse>> verifyResetOtp(@Body OtpVerifyRequest request);

    @POST("auth/reset-password")
    Call<BaseResponse<String>> resetPassword(@Body ResetPasswordRequest request);

    @POST("auth/refresh-token")
    Call<BaseResponse<AuthResponse>> refreshToken(@Body RefreshTokenRequest request);

    @POST("auth/logout")
    Call<BaseResponse<Void>> logout(@Body LogoutRequest request);

    @POST("auth/update-fcm")
    Call<BaseResponse<Void>> updateFcmToken(
        @Header("Authorization") String authorization,
        @Body UpdateFcmTokenRequest request
    );
}
