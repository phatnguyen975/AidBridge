package com.drc.aidbridge.infrastructure.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.jspecify.annotations.NonNull;
import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        try {
            String jwtToken = extractJwtFromRequest(request);
            log.info("Extracted JWT: {}", jwtToken); // Debug log
            log.info("StringUtils.hasText(jwt): {}", StringUtils.hasText(jwtToken));
            log.info("Current Authentication: {}", SecurityContextHolder.getContext().getAuthentication());

            if (StringUtils.hasText(jwtToken) && SecurityContextHolder.getContext().getAuthentication() == null) {
                Claims claims = jwtService.validateAccessToken(jwtToken);
                UUID userId = jwtService.extractUserId(claims);
                String role = jwtService.extractRole(claims);

                log.info("Authenticated user ID: {}, role: {}", userId, role); // Debug log

                List<SimpleGrantedAuthority> authorities = role == null || role.isBlank()
                        ? List.of()
                        : List.of(new SimpleGrantedAuthority("ROLE_" + role));

                // Create Spring Security Jwt object for @AuthenticationPrincipal compatibility
                Jwt jwt = createJwtFromClaims(jwtToken, claims, userId);

                // Use JwtAuthenticationToken instead of UsernamePasswordAuthenticationToken
                JwtAuthenticationToken authentication = new JwtAuthenticationToken(
                        jwt,
                        authorities);
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (JwtException ex) {
            log.debug("JWT validation failed: {}", ex.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Create Spring Security Jwt object from JJWT claims
     * This allows @AuthenticationPrincipal Jwt to work in controllers
     */
    private Jwt createJwtFromClaims(String tokenValue, Claims claims, UUID userId) {
        Map<String, Object> headers = new HashMap<>();
        headers.put("alg", "RS256");
        headers.put("typ", "JWT");

        Instant issuedAt = claims.getIssuedAt() != null
                ? claims.getIssuedAt().toInstant()
                : Instant.now();
        Instant expiresAt = claims.getExpiration() != null
                ? claims.getExpiration().toInstant()
                : Instant.now().plusSeconds(3600);

        // Convert JJWT Claims to Map for Spring Security Jwt
        Map<String, Object> claimsMap = new HashMap<>();
        claims.forEach((key, value) -> claimsMap.put(key, value));

        // Ensure 'sub' claim contains userId as string
        claimsMap.put("sub", userId.toString());

        return Jwt.withTokenValue(tokenValue)
                .headers(h -> h.putAll(headers))
                .claims(c -> c.putAll(claimsMap))
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .build();
    }

    private String extractJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/api/auth/")
                || path.startsWith("/api/health/")
                || path.equals("/actuator/health")
                || path.startsWith("/ws/");
    }
}
