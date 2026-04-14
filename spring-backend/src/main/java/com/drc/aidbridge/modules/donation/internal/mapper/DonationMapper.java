package com.drc.aidbridge.modules.donation.internal.mapper;

import com.drc.aidbridge.modules.donation.DonationDTO;
import com.drc.aidbridge.modules.donation.internal.entity.Donation;
import com.drc.aidbridge.modules.donation.internal.entity.DonationItem;
import org.springframework.stereotype.Component;

@Component
public class DonationMapper {

    public DonationDTO toDTO(Donation e) {
        if (e == null) return null;
        return DonationDTO.builder()
                .id(e.getId())
                .sponsorId(e.getSponsorId())
                .hubId(e.getHubId())
            .qrCodeToken(e.getQrCodeToken())
                .status(e.getStatus())
                .notes(e.getNotes())
                .receivedAt(e.getReceivedAt())
                .receivedBy(e.getReceivedBy())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }

    public DonationDTO.DonationItemDTO toItemDTO(DonationItem item) {
        if (item == null) {
            return null;
        }

        return DonationDTO.DonationItemDTO.builder()
                .id(item.getId())
                .itemName(item.getItemName())
                .itemCategoryId(item.getItemCategoryId())
                .quantity(item.getQuantity())
                .unit(item.getUnit())
                .description(item.getDescription())
                .expiryDate(item.getExpiryDate())
                .imageUrl(item.getImageUrl())
                .createdAt(item.getCreatedAt())
                .build();
    }
}
