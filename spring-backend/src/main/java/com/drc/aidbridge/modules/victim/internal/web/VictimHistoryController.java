package com.drc.aidbridge.modules.victim.internal.web;

import com.drc.aidbridge.modules.shared.dto.ApiResponse;
import com.drc.aidbridge.modules.shared.exception.AuthenticationException;
import com.drc.aidbridge.modules.victim.internal.usecase.GetVictimHistoryDetailUseCase;
import com.drc.aidbridge.modules.victim.internal.usecase.GetVictimHistoryUseCase;
import com.drc.aidbridge.modules.victim.internal.web.dto.VictimHistoryDetailResponse;
import com.drc.aidbridge.modules.victim.internal.web.dto.VictimHistoryPageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Provides victim request history for SOS and aid submissions.
 */
@RestController
@RequestMapping("/api/victim")
@RequiredArgsConstructor
public class VictimHistoryController {

    private final GetVictimHistoryUseCase getVictimHistoryUseCase;
    private final GetVictimHistoryDetailUseCase getVictimHistoryDetailUseCase;

    /**
     * Returns paginated unified history for the authenticated victim account.
     */
    @GetMapping("/history")
    public ResponseEntity<ApiResponse<VictimHistoryPageResponse>> getHistory(
        @RequestParam(name = "page", defaultValue = "1") int page,
        @RequestParam(name = "size", defaultValue = "10") int size,
        @RequestParam(name = "timeRange", defaultValue = "1h") String timeRange,
        Authentication authentication
    ) {
        UUID userId = resolveUserId(authentication);
        VictimHistoryPageResponse response = getVictimHistoryUseCase.execute(userId, page, size, timeRange);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Returns detail payload for one request in victim history.
     */
    @GetMapping("/history/{requestId}/detail")
    public ResponseEntity<ApiResponse<VictimHistoryDetailResponse>> getHistoryDetail(
        @PathVariable UUID requestId,
        @RequestParam(name = "type", required = false) String type,
        Authentication authentication
    ) {
        UUID userId = resolveUserId(authentication);
        VictimHistoryDetailResponse response = getVictimHistoryDetailUseCase.execute(userId, requestId, type);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    private UUID resolveUserId(Authentication authentication) {
        if (authentication == null
            || !authentication.isAuthenticated()
            || "anonymousUser".equals(authentication.getName())) {
            throw new AuthenticationException("Unauthorized request");
        }

        String userIdCandidate = authentication.getName();
        if ((userIdCandidate == null || userIdCandidate.isBlank())
            && authentication.getPrincipal() instanceof Jwt jwt) {
            userIdCandidate = jwt.getSubject();
        }

        if (userIdCandidate == null || userIdCandidate.isBlank()) {
            throw new AuthenticationException("Invalid authentication context");
        }

        try {
            return UUID.fromString(userIdCandidate);
        } catch (IllegalArgumentException ex) {
            throw new AuthenticationException("Invalid authentication context");
        }
    }
}
