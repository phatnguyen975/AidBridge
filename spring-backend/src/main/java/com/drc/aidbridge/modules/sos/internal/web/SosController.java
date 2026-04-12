package com.drc.aidbridge.modules.sos.internal.web;

import com.drc.aidbridge.modules.shared.dto.ApiResponse;
import com.drc.aidbridge.modules.shared.exception.AuthenticationException;
import com.drc.aidbridge.modules.sos.internal.usecase.CreateGuestSosRequestUseCase;
import com.drc.aidbridge.modules.sos.internal.usecase.CreateSosRequestUseCase;
import com.drc.aidbridge.modules.sos.internal.usecase.GetSosRequestUseCase;
import com.drc.aidbridge.modules.sos.internal.usecase.ListSosRequestsUseCase;
import com.drc.aidbridge.modules.sos.internal.web.dto.CreateGuestSosRequest;
import com.drc.aidbridge.modules.sos.internal.web.dto.CreateSosRequest;
import com.drc.aidbridge.modules.sos.internal.web.dto.SosRequestResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/sos-requests")
@RequiredArgsConstructor
public class SosController {

    private final CreateSosRequestUseCase createSosRequestUseCase;
    private final CreateGuestSosRequestUseCase createGuestSosRequestUseCase;
    private final GetSosRequestUseCase getSosRequestUseCase;
    private final ListSosRequestsUseCase listSosRequestsUseCase;

    @PostMapping
    public ResponseEntity<ApiResponse<SosRequestResponse>> createSosRequest(
            @Valid @RequestBody CreateSosRequest request,
            Authentication authentication) {

        UUID userId = resolveAuthenticatedUserId(authentication);

        SosRequestResponse response;
        if (userId != null) {
            response = createSosRequestUseCase.execute(userId, request);
        } else {
            CreateGuestSosRequest guestRequest = CreateGuestSosRequest.from(request);
            response = createGuestSosRequestUseCase.execute(guestRequest);
        }

        return ResponseEntity.ok(ApiResponse.success("SOS request created", response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SosRequestResponse>> getSosRequest(@PathVariable UUID id) {
        SosRequestResponse response = getSosRequestUseCase.execute(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<SosRequestResponse>>> listSosRequests() {
        List<SosRequestResponse> response = listSosRequestsUseCase.execute();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    private UUID resolveAuthenticatedUserId(Authentication authentication) {
        if (authentication == null
            || !authentication.isAuthenticated()
            || "anonymousUser".equals(authentication.getName())) {
            return null;
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
