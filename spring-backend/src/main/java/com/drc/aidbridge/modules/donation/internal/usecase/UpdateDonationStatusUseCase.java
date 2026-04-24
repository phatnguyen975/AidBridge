package com.drc.aidbridge.modules.donation.internal.usecase;

import com.drc.aidbridge.modules.donation.DonationDTO;
import com.drc.aidbridge.modules.donation.internal.entity.Donation;
import com.drc.aidbridge.modules.donation.internal.mapper.DonationMapper;
import com.drc.aidbridge.modules.donation.internal.repository.DonationItemRepository;
import com.drc.aidbridge.modules.donation.internal.repository.DonationRepository;
import com.drc.aidbridge.modules.donation.internal.web.dto.UpdateDonationStatusRequest;
import com.drc.aidbridge.modules.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UpdateDonationStatusUseCase {

    private final DonationRepository donationRepository;
    private final DonationItemRepository donationItemRepository;
    private final DonationMapper donationMapper;

    @Transactional
    public DonationDTO execute(UUID id, UpdateDonationStatusRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request is null");
        }

        Donation donation = donationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Donation not found: " + id));

        donation.setStatus(request.getStatus());
        if (request.getNotes() != null) {
            donation.setNotes(request.getNotes());
        }

        Donation savedDonation = donationRepository.save(donation);
        DonationDTO dto = donationMapper.toDTO(savedDonation);
        dto.setItems(donationItemRepository.findAllByDonationId(savedDonation.getId()).stream()
                .map(donationMapper::toItemDTO)
                .toList());
        return dto;
    }
}
