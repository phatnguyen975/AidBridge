package com.drc.aidbridge.modules.donation.internal.web;

import com.drc.aidbridge.modules.donation.DonationDTO;
import com.drc.aidbridge.modules.donation.DonationFacade;
import com.drc.aidbridge.modules.donation.internal.usecase.GetDonationByIdUseCase;
import com.drc.aidbridge.modules.donation.internal.web.dto.CreateDonationRequest;
import com.drc.aidbridge.modules.donation.internal.web.dto.UpdateDonationStatusRequest;
import com.drc.aidbridge.modules.shared.dto.ApiResponse;
import com.drc.aidbridge.modules.shared.dto.PaginatedResponseDto;
import com.drc.aidbridge.modules.shared.enums.DonationStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/donations")
@RequiredArgsConstructor
public class DonationController {

    private final DonationFacade donationFacade;
    private final GetDonationByIdUseCase getDonationByIdUseCase;

    @PostMapping
    public ResponseEntity<ApiResponse<DonationDTO>> createDonation(
            Authentication authentication,
            @Valid @RequestBody CreateDonationRequest request) {
        UUID sponsorId = UUID.fromString(authentication.getName());
        DonationDTO dto = donationFacade.create(sponsorId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Donation created successfully", dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DonationDTO>> getDonation(@PathVariable UUID id) {
        DonationDTO dto = getDonationByIdUseCase.execute(id);
        if (dto == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(ApiResponse.success("Donation retrieved successfully", dto));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PaginatedResponseDto<DonationDTO>>> listDonations(
            @RequestParam(required = false) DonationStatus status,
            @RequestParam(name = "hub_id", required = false) UUID hubId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit) {
        PaginatedResponseDto<DonationDTO> response = donationFacade.list(status, hubId, page, limit);
        return ResponseEntity.ok(ApiResponse.success("Donations retrieved successfully", response));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<DonationDTO>> updateDonationStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateDonationStatusRequest request) {
        DonationDTO dto = donationFacade.updateStatus(id, request);
        return ResponseEntity.ok(ApiResponse.success("Donation status updated successfully", dto));
    }
}
