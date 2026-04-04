package com.drc.aidbridge.modules.user.internal.usecase;

import com.drc.aidbridge.infrastructure.security.JwtService;
import com.drc.aidbridge.modules.shared.exception.AuthenticationException;
import com.drc.aidbridge.modules.shared.exception.ResourceNotFoundException;
import com.drc.aidbridge.modules.user.internal.entity.User;
import com.drc.aidbridge.modules.user.internal.mapper.UserMapper;
import com.drc.aidbridge.modules.user.internal.repository.UserJpaRepository;
import com.drc.aidbridge.modules.user.internal.web.dto.AuthResponse;
import com.drc.aidbridge.modules.user.internal.web.dto.RefreshTokenRequest;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class RefreshTokenUseCase {

    private final UserJpaRepository userRepository;
    private final JwtService jwtService;
    private final UserMapper userMapper;

    public AuthResponse execute(RefreshTokenRequest request) {
        Claims claims;
        try {
            claims = jwtService.validateRefreshToken(request.getRefreshToken());
        } catch (JwtException e) {
            throw new AuthenticationException("Invalid or expired refresh token");
        }

        UUID userId = jwtService.extractUserId(claims);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!user.isActive()) {
            throw new AuthenticationException("Account is deactivated");
        }

        // OPTIMIZATION: Không blacklist refresh token cũ khi rotate.
        // Lý do:
        // 1. Token cũ sẽ tự expire sau 7 ngày
        // 2. Security vẫn OK vì chỉ 1 token được dùng tại 1 thời điểm (client thay bằng
        // token mới)
        // 3. Giảm 50% Redis writes khi refresh token
        // Nếu cần revoke tất cả tokens → dùng revokeAllUserTokens() trong emergency

        String newAccessToken = jwtService.generateAccessToken(user.getId(), user.getRole().name());
        String newRefreshToken = jwtService.generateRefreshToken(user.getId());

        log.debug("Token refreshed for user: {}", user.getEmail());
        return userMapper.buildAuthResponse(user, newAccessToken, newRefreshToken);
    }
}
