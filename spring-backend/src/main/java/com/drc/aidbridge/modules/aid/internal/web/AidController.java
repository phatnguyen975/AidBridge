package com.drc.aidbridge.modules.aid.internal.web;
import com.drc.aidbridge.modules.shared.dto.ApiResponse;
import com.drc.aidbridge.modules.shared.dto.PaginatedResponseDto;
import com.drc.aidbridge.modules.shared.exception.AuthenticationException;
import com.drc.aidbridge.modules.aid.internal.usecase.*;
import com.drc.aidbridge.modules.aid.internal.web.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/aid-requests")
@RequiredArgsConstructor
public class AidController {

    private final CreateAidRequestUseCase createAidRequestUseCase;
    private final GetAidRequestUseCase getAidRequestUseCase;
    private final CancelAidRequestUseCase cancelAidRequestUseCase;
    private final ListAidRequestsUseCase listAidRequestsUseCase;
    private final TranscribeAidRequestVoiceUseCase transcribeAidRequestVoiceUseCase;

    @PostMapping
    public ResponseEntity<ApiResponse<AidRequestResponse>> createAidRequest(
            @Valid @RequestBody CreateAidRequest request,
            Authentication authentication) {
        UUID userId = resolveAuthenticatedUserId(authentication);
        AidRequestResponse response = createAidRequestUseCase.execute(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Aid request created", response));
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AidRequestResponse>> getAidRequest(@PathVariable UUID id) {
        AidRequestResponse response = getAidRequestUseCase.execute(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<AidRequestResponse>> cancelAidRequest(
            @PathVariable UUID id,
            Authentication authentication,
            @RequestBody(required = false) CancelAidRequest request) {
        UUID userId = resolveAuthenticatedUserId(authentication);
        if (request == null) {
            request = new CancelAidRequest();
        }
        AidRequestResponse response = cancelAidRequestUseCase.execute(userId, id, request);
        return ResponseEntity.ok(ApiResponse.success("Aid request cancelled", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PaginatedResponseDto<AidRequestResponse>>> listAidRequests(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "limit", defaultValue = "20") int limit) {
        PaginatedResponseDto<AidRequestResponse> response = listAidRequestsUseCase.execute(page, limit);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping(value = "/voice", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<String>> transcribeVoice(
            @RequestPart("file") org.springframework.web.multipart.MultipartFile audioFile) {

        String transcript = transcribeAidRequestVoiceUseCase.execute(audioFile);
        return ResponseEntity.ok(ApiResponse.success("Voice transcription completed", transcript));
    }

    private UUID resolveAuthenticatedUserId(Authentication authentication) {
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
