package com.drc.aidbridge.modules.victim.internal.web;

import com.drc.aidbridge.modules.shared.dto.ApiResponse;
import com.drc.aidbridge.modules.shared.exception.AuthenticationException;
import com.drc.aidbridge.modules.victim.internal.usecase.GetVictimHistoryUseCase;
import com.drc.aidbridge.modules.victim.internal.web.dto.VictimHistoryPageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
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

    private UUID resolveUserId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new AuthenticationException("Unauthorized request");
        }

        try {
            return UUID.fromString(authentication.getName());
        } catch (IllegalArgumentException ex) {
            throw new AuthenticationException("Invalid authentication context");
        }
    }
}
