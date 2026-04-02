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
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String accessToken = authHeader.substring(7);
            jwtService.revokeToken(accessToken);
        }

        if (request != null && request.getRefreshToken() != null) {
            jwtService.revokeToken(request.getRefreshToken());
        }

        log.info("User logged out");
    }
}
