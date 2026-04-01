package com.drc.aidbridge.data.remote.interceptor;

import com.drc.aidbridge.data.remote.api.AuthApiService;
import com.drc.aidbridge.data.remote.dto.response.AuthResponse;
import com.drc.aidbridge.data.remote.dto.response.BaseResponse;
import com.drc.aidbridge.utils.Constants;
import com.drc.aidbridge.utils.TokenManager;
import com.drc.aidbridge.ui.auth.AuthActivity;

import android.content.Intent;
import android.content.Context;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.Lazy;
import dagger.hilt.android.qualifiers.ApplicationContext;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
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

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final TokenManager tokenManager;
    private final Lazy<AuthApiService> lazyAuthApiService;
    private final Context applicationContext;

    private final Object refreshLock = new Object();

    @Inject
    public TokenRefreshInterceptor(TokenManager tokenManager,
                                   Lazy<AuthApiService> lazyAuthApiService,
                                   @ApplicationContext Context applicationContext) {
        this.tokenManager = tokenManager;
        this.lazyAuthApiService = lazyAuthApiService;
        this.applicationContext = applicationContext;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request originalRequest = chain.request();
        Response response = chain.proceed(originalRequest);

        if (response.code() != 401 || isRefreshRequest(originalRequest)) {
            return response;
        }

        String requestAccessToken = extractTokenFromHeader(originalRequest.header(AUTHORIZATION_HEADER));

        // Close immediately to prevent leaking sockets while we perform refresh/retry logic.
        response.close();

        synchronized (refreshLock) {
            String latestAccessToken = tokenManager.getAccessToken();

            // Another request may have already refreshed the token while this thread waited.
            if (shouldRetryWithExistingToken(requestAccessToken, latestAccessToken)) {
                return retryRequest(chain, originalRequest, latestAccessToken);
            }

            String refreshToken = tokenManager.getRefreshToken();
            if (isBlank(refreshToken)) {
                return forceLogout(originalRequest);
            }

            AuthResponse refreshedAuth = refreshTokenBlocking(refreshToken);
            if (refreshedAuth == null || isBlank(refreshedAuth.getAccessToken())) {
                return forceLogout(originalRequest);
            }

            String nextRefreshToken = isBlank(refreshedAuth.getRefreshToken())
                ? refreshToken
                : refreshedAuth.getRefreshToken();
            tokenManager.saveTokens(refreshedAuth.getAccessToken(), nextRefreshToken);

            return retryRequest(chain, originalRequest, refreshedAuth.getAccessToken());
        }
    }

    private boolean isRefreshRequest(Request request) {
        return request.url().encodedPath().contains(Constants.REFRESH_TOKEN_ENDPOINT);
    }

    private boolean shouldRetryWithExistingToken(String requestAccessToken, String latestAccessToken) {
        if (isBlank(latestAccessToken)) {
            return false;
        }

        return isBlank(requestAccessToken) || !latestAccessToken.equals(requestAccessToken);
    }

    private AuthResponse refreshTokenBlocking(String refreshToken) {
        Map<String, String> body = new HashMap<>();
        body.put("refreshToken", refreshToken);

        try {
            Call<BaseResponse<AuthResponse>> refreshCall = lazyAuthApiService.get().refreshToken(body);
            retrofit2.Response<BaseResponse<AuthResponse>> refreshResponse = refreshCall.execute();

            if (!refreshResponse.isSuccessful()) {
                return null;
            }

            BaseResponse<AuthResponse> baseResponse = refreshResponse.body();
            if (baseResponse == null || !baseResponse.isSuccess()) {
                return null;
            }

            return baseResponse.getData();
        } catch (IOException refreshException) {
            return null;
        }
    }

    private Response retryRequest(Chain chain, Request originalRequest, String accessToken) throws IOException {
        Request retryRequest = originalRequest.newBuilder()
            .header(AUTHORIZATION_HEADER, BEARER_PREFIX + accessToken)
            .build();
        return chain.proceed(retryRequest);
    }

    private Response forceLogout(Request originalRequest) {
        tokenManager.clearAll();
        
        Intent intent = new Intent(applicationContext, AuthActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        applicationContext.startActivity(intent);

        MediaType jsonMediaType = MediaType.get("application/json; charset=utf-8");
        return new Response.Builder()
            .request(originalRequest)
            .protocol(Protocol.HTTP_1_1)
            .code(401)
            .message("Unauthorized")
            .body(ResponseBody.Companion.create("", jsonMediaType))
            .build();
    }

    private String extractTokenFromHeader(String authorizationHeader) {
        if (isBlank(authorizationHeader) || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            return null;
        }

        String token = authorizationHeader.substring(BEARER_PREFIX.length());
        return isBlank(token) ? null : token;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
