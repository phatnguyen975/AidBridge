package com.drc.aidbridge.modules.donation.internal.web;

import com.drc.aidbridge.modules.donation.DonationDTO;
import com.drc.aidbridge.modules.donation.internal.usecase.GetDonationByIdUseCase;
import com.drc.aidbridge.modules.shared.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/donations")
@RequiredArgsConstructor
public class DonationController {

    private final GetDonationByIdUseCase getDonationByIdUseCase;

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DonationDTO>> getDonation(@PathVariable UUID id) {
        DonationDTO dto = getDonationByIdUseCase.execute(id);
        if (dto == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(ApiResponse.success("Donation retrieved successfully", dto));
    }
}
