package com.drc.aidbridge.data.remote.interceptor;

import com.drc.aidbridge.data.remote.api.AuthApiService;
import com.drc.aidbridge.data.remote.dto.response.AuthResponse;
import com.drc.aidbridge.utils.Constants;
import com.drc.aidbridge.utils.TokenManager;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.Lazy;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Call;

/**
 * TokenRefreshInterceptor — OkHttp interceptor that handles automatic JWT token renewal.
 *
 * IMPORTANT: AuthApiService is injected as {@code dagger.Lazy<AuthApiService>} to break
 * the Dagger circular dependency:
 *   TokenRefreshInterceptor → AuthApiService → Retrofit → OkHttpClient → TokenRefreshInterceptor
 *
 * Using Lazy<> defers the creation of AuthApiService until the first token refresh attempt,
 * which happens after the full OkHttpClient + Retrofit graph has already been built.
 *
 * Workflow:
 * 1. Proceed with the original request.
 * 2. If the response is 401 AND this isn't already a refresh request:
 *    a. Call the refresh-token endpoint synchronously (blocking, background thread).
 *    b. Save new tokens via TokenManager.
 *    c. Retry the original request with the new access token.
 * 3. If the refresh token is also expired/invalid:
 *    a. Clear all tokens (force logout).
 *    b. Return the original 401 response (user must log in again).
 */
@Singleton
public class TokenRefreshInterceptor implements Interceptor {

    private final TokenManager tokenManager;
    // Lazy<> breaks the circular dep: AuthApiService is only created on first .get() call,
    // which is after OkHttpClient + Retrofit are fully initialised.
    private final Lazy<AuthApiService> lazyAuthApiService;

    @Inject
    public TokenRefreshInterceptor(TokenManager tokenManager,
                                   Lazy<AuthApiService> lazyAuthApiService) {
        this.tokenManager = tokenManager;
        this.lazyAuthApiService = lazyAuthApiService;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request originalRequest = chain.request();
        Response response = chain.proceed(originalRequest);

        // Only intercept 401 errors that are NOT from the refresh endpoint itself
        if (response.code() == 401
                && !originalRequest.url().encodedPath()
                        .contains(Constants.REFRESH_TOKEN_ENDPOINT)) {
            String refreshToken = tokenManager.getRefreshToken();
            if (refreshToken == null) {
                // No refresh token available — user must log in again
                return response;
            }

            // Build the refresh request body
            Map<String, String> body = new HashMap<>();
            body.put("refreshToken", refreshToken);

            // Resolve AuthApiService now (safe — Retrofit is fully constructed by this point)
            Call<AuthResponse> refreshCall = lazyAuthApiService.get().refreshToken(body);
            retrofit2.Response<AuthResponse> refreshResponse = refreshCall.execute();

            if (refreshResponse.isSuccessful() && refreshResponse.body() != null) {
                response.close();

                AuthResponse authResponse = refreshResponse.body();

                // Save the newly obtained tokens to EncryptedSharedPreferences
                tokenManager.saveTokens(
                        authResponse.getAccessToken(),
                        authResponse.getRefreshToken()
                );

                // Retry the original failed request with the new access token
                Request retryRequest = originalRequest.newBuilder()
                        .header("Authorization", "Bearer " + authResponse.getAccessToken())
                        .build();
                return chain.proceed(retryRequest);
            } else {
                // Refresh token also rejected — clear session and force re-login
                tokenManager.clearAll();
                return response;
            }
        }

        return response;
    }
}
