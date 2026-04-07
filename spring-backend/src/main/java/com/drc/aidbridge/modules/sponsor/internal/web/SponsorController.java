package com.drc.aidbridge.modules.sponsor.internal.web;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import com.drc.aidbridge.modules.shared.dto.ApiResponse;
import com.drc.aidbridge.modules.sponsor.internal.web.dto.SponsorProfileResponse;
import com.drc.aidbridge.modules.sponsor.internal.usecase.GetSponsorProfileUseCase;
import com.drc.aidbridge.modules.sponsor.internal.usecase.UpdateSponsorProfileUseCase;
import com.drc.aidbridge.modules.sponsor.internal.web.dto.UpdateSponsorRequest;
import org.springframework.security.core.Authentication;
import java.util.UUID;

@RestController
@RequestMapping("/api/sponsors")
@RequiredArgsConstructor
public class SponsorController {
    
    private final GetSponsorProfileUseCase getSponsorProfileUseCase;
    private final UpdateSponsorProfileUseCase updateSponsorProfileUseCase;

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<SponsorProfileResponse>> getSponsorProfile(
        Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        SponsorProfileResponse dto = getSponsorProfileUseCase.execute(userId);
        if (dto == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(ApiResponse.success("Sponsor profile retrieved successfully", dto));
    }

    @PatchMapping("/profile")
    public ResponseEntity<ApiResponse<SponsorProfileResponse>> updateSponsorProfile(
            Authentication authentication,
            @RequestBody UpdateSponsorRequest request) {
        UUID userId = UUID.fromString(authentication.getName());
        SponsorProfileResponse dto = updateSponsorProfileUseCase.execute(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Sponsor profile updated successfully", dto));
    }

}
