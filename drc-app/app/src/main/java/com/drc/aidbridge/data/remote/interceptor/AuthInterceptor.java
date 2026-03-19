package com.drc.aidbridge.data.remote.interceptor;

import com.drc.aidbridge.utils.Constants;
import com.drc.aidbridge.utils.TokenManager;

import java.io.IOException;
import javax.inject.Inject;
import javax.inject.Singleton;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * AuthInterceptor — OkHttp interceptor that attaches the JWT Bearer token
 * to all outgoing requests that require authentication.
 *
 * Logic:
 * 1. Skip /auth/* paths (login, register, OTP) — these are public endpoints.
 * 2. For all other requests, read the access token from TokenManager and
 *    attach it as the "Authorization: Bearer <token>" header.
 *
 * If no token is present (user is not logged in), the request is sent without
 * an Authorization header; the server will respond with 401 accordingly.
 */
@Singleton
public class AuthInterceptor implements Interceptor {

    private final TokenManager tokenManager;

    @Inject
    public AuthInterceptor(TokenManager tokenManager) {
        this.tokenManager = tokenManager;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request originalRequest = chain.request();
        String path = originalRequest.url().encodedPath();

        // Skip auth header injection for public /auth/* endpoints
        if (path.contains(Constants.AUTH_PATH_PREFIX)) {
            return chain.proceed(originalRequest);
        }

        String token = tokenManager.getAccessToken();
        if (token == null || token.isEmpty()) {
            // No token — proceed without auth header (will likely 401 on protected endpoints)
            return chain.proceed(originalRequest);
        }

        // Attach the Bearer token to the Authorization header
        Request authenticatedRequest = originalRequest.newBuilder()
                .header("Authorization", "Bearer " + token)
                .build();

        return chain.proceed(authenticatedRequest);
    }
}
