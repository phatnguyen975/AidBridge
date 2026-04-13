package com.drc.aidbridge.modules.aid.internal.web;

import com.drc.aidbridge.modules.shared.dto.ApiResponse;
import com.drc.aidbridge.modules.shared.dto.PaginatedResponseDto;
import com.drc.aidbridge.modules.shared.exception.BadRequestException;
import com.drc.aidbridge.modules.aid.internal.usecase.*;
import com.drc.aidbridge.modules.aid.internal.web.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
            @AuthenticationPrincipal Jwt jwt) {
        UUID userId = resolveUserId(jwt);
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
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody(required = false) CancelAidRequest request) {
        UUID userId = resolveUserId(jwt);
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
    public ResponseEntity<ApiResponse<AidRequestResponse>> transcribeVoice(
            @RequestPart("file") org.springframework.web.multipart.MultipartFile audioFile,
            @Valid @ModelAttribute CreateAidRequestVoiceInput request,
            @AuthenticationPrincipal Jwt jwt) {
        UUID userId = resolveUserId(jwt);
        System.out.println(System.getProperty("file.encoding"));
        AidRequestResponse response = transcribeAidRequestVoiceUseCase.execute(userId, audioFile, request);
        return ResponseEntity.ok(ApiResponse.success("Aid request created", response));
    }

    private UUID resolveUserId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
