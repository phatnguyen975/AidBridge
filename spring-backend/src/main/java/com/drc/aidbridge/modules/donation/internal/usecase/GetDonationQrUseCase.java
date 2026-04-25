package com.drc.aidbridge.modules.donation.internal.usecase;

import com.drc.aidbridge.modules.donation.internal.entity.Donation;
import com.drc.aidbridge.modules.donation.internal.repository.DonationRepository;
import com.drc.aidbridge.modules.donation.internal.web.dto.DonationQrResponse;
import com.drc.aidbridge.modules.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class GetDonationQrUseCase {

    private final DonationRepository donationRepository;

    public DonationQrResponse execute(UUID donationId) {
        Donation donation = donationRepository.findById(donationId)
                .orElseThrow(() -> new ResourceNotFoundException("Donation not found: " + donationId));

        return DonationQrResponse.builder()
                .donationId(donation.getId())
                .donationCode(donation.getDonationCode())
                .qrCodeToken(donation.getQrCodeToken())
                .build();
    }
}
