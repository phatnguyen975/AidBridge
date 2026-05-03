package com.drc.aidbridge.modules.donation.internal.mapper;

import com.drc.aidbridge.modules.aid.internal.repository.AidItemCategoryJpaRepository;
import com.drc.aidbridge.modules.donation.DonationDTO;
import com.drc.aidbridge.modules.donation.internal.entity.Donation;
import com.drc.aidbridge.modules.donation.internal.entity.DonationItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DonationMapper {

    private final AidItemCategoryJpaRepository aidItemCategoryJpaRepository;

    public DonationDTO toDTO(Donation e) {
        if (e == null) return null;
        return DonationDTO.builder()
                .id(e.getId())
                .sponsorId(e.getSponsorId())
                .hubId(e.getHubId())
                .qrCodeToken(e.getQrCodeToken())
                .status(e.getStatus())
                .receivedAt(e.getReceivedAt())
                .receivedBy(e.getReceivedBy())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .donationCode(e.getDonationCode())
                .build();
    }

    public DonationDTO.DonationItemDTO toItemDTO(DonationItem item) {
        if (item == null) {
            return null;
        }

        String categoryName = null;
        if (item.getItemCategoryId() != null) {
            categoryName = aidItemCategoryJpaRepository.findById(item.getItemCategoryId())
                    .map(category -> category.getName() != null ? category.getName().trim() : null)
                    .orElse(null);
        }

        return DonationDTO.DonationItemDTO.builder()
                .id(item.getId())
                .itemCategoryId(item.getItemCategoryId())
                .itemCategoryName(categoryName)
                .createdAt(item.getCreatedAt())
                .build();
    }
}
