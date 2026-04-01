package com.drc.aidbridge.modules.sos.internal.web;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.drc.aidbridge.modules.shared.dto.ApiResponse;
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
            @AuthenticationPrincipal UUID userId) {

        SosRequestResponse response;
        if (userId != null) {
            response = createSosRequestUseCase.execute(userId, request);
        } else {
            CreateGuestSosRequest guestRequest = CreateGuestSosRequest.builder()
                    .lat(request.getLat())
                    .lng(request.getLng())
                    .address(request.getAddress())
                    .description(request.getDescription())
                    .peopleCount(request.getPeopleCount())
                    .urgencyLevel(request.getUrgencyLevel())
                    .imageUrl(request.getImageUrl())
                    .build();
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
}
