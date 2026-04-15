package com.drc.aidbridge.modules.donation.internal.web;

import com.drc.aidbridge.modules.donation.DonationDTO;
import com.drc.aidbridge.modules.donation.DonationFacade;
import com.drc.aidbridge.modules.donation.internal.usecase.GetDonationByIdUseCase;
import com.drc.aidbridge.modules.donation.internal.web.dto.CreateDonationRequest;
import com.drc.aidbridge.modules.shared.dto.ApiResponse;
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
}
