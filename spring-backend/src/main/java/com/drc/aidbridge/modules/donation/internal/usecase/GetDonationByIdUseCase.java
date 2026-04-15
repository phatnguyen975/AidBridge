package com.drc.aidbridge.modules.donation.internal.usecase;

import com.drc.aidbridge.modules.donation.DonationDTO;
import com.drc.aidbridge.modules.donation.internal.mapper.DonationMapper;
import com.drc.aidbridge.modules.donation.internal.repository.DonationItemRepository;
import com.drc.aidbridge.modules.donation.internal.repository.DonationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class GetDonationByIdUseCase {

    private final DonationRepository donationRepository;
    private final DonationItemRepository donationItemRepository;
    private final DonationMapper donationMapper;

    public DonationDTO execute(UUID id) {
        return donationRepository.findById(id)
                .map(donation -> {
                    DonationDTO dto = donationMapper.toDTO(donation);
                    dto.setItems(donationItemRepository.findAllByDonationId(donation.getId()).stream()
                            .map(donationMapper::toItemDTO)
                            .toList());
                    return dto;
                })
                .orElse(null);
    }
}
