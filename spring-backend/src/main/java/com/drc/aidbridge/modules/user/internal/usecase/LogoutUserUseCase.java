package com.drc.aidbridge.modules.user.internal.usecase;

import com.drc.aidbridge.infrastructure.security.JwtService;
import com.drc.aidbridge.modules.user.internal.web.dto.LogoutRequest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LogoutUserUseCase {

    private final JwtService jwtService;

    public void execute(String authHeader, LogoutRequest request) {
        // OPTIMIZATION: Chỉ blacklist refresh token (TTL dài).
        // Access token có TTL ngắn (15 phút) nên để tự hết hạn, tránh ghi Redis không
        // cần thiết.

        if (request != null && request.getRefreshToken() != null) {
            jwtService.revokeToken(request.getRefreshToken());
            log.info("Refresh token revoked during logout");
        }

        // Access token không cần blacklist - sẽ tự expire sau 15 phút
        log.info("User logged out (access token will expire naturally)");
    }
}
