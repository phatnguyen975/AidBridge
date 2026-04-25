package com.drc.aidbridge.modules.donation.internal.usecase;

import com.drc.aidbridge.modules.aid.internal.repository.AidItemCategoryJpaRepository;
import com.drc.aidbridge.modules.donation.DonationDTO;
import com.drc.aidbridge.modules.donation.internal.entity.Donation;
import com.drc.aidbridge.modules.donation.internal.entity.DonationItem;
import com.drc.aidbridge.modules.donation.internal.mapper.DonationMapper;
import com.drc.aidbridge.modules.donation.internal.repository.DonationItemRepository;
import com.drc.aidbridge.modules.donation.internal.repository.DonationRepository;
import com.drc.aidbridge.modules.donation.internal.web.dto.CreateDonationItemRequest;
import com.drc.aidbridge.modules.donation.internal.web.dto.CreateDonationRequest;
import com.drc.aidbridge.modules.hub.internal.repository.HubRepository;
import com.drc.aidbridge.modules.shared.enums.DonationStatus;
import com.drc.aidbridge.modules.shared.exception.ResourceNotFoundException;
import com.drc.aidbridge.modules.user.internal.repository.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CreateDonationUseCase {

    private final DonationRepository donationRepository;
    private final DonationItemRepository donationItemRepository;
    private final UserJpaRepository userJpaRepository;
    private final HubRepository hubRepository;
    private final AidItemCategoryJpaRepository aidItemCategoryJpaRepository;
    private final DonationMapper donationMapper;

    @Transactional
    public DonationDTO execute(UUID sponsorId, CreateDonationRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request is null");
        }

        if (!userJpaRepository.existsById(sponsorId)) {
            throw new ResourceNotFoundException("Sponsor user not found: " + sponsorId);
        }

        if (!hubRepository.existsById(request.getHubId())) {
            throw new ResourceNotFoundException("Hub not found: " + request.getHubId());
        }

        Donation donation = donationRepository.save(Donation.builder()
                .sponsorId(sponsorId)
                .hubId(request.getHubId())
                .qrCodeToken(generateUniqueQrCodeToken())
                .donationCode(generateDonationCode())   
                .status(DonationStatus.REGISTERED)
                .build());

        Set<UUID> selectedCategoryIds = request.getItems().stream()
            .map(CreateDonationItemRequest::getItemCategoryId)
            .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));

        List<DonationItem> items = selectedCategoryIds.stream()
            .map(categoryId -> toEntity(donation.getId(), categoryId))
                .toList();

        List<DonationItem> savedItems = donationItemRepository.saveAll(items);

        DonationDTO dto = donationMapper.toDTO(donation);
        dto.setItems(savedItems.stream().map(donationMapper::toItemDTO).toList());
        return dto;
    }

    private DonationItem toEntity(UUID donationId, UUID itemCategoryId) {
        if (itemCategoryId != null && !aidItemCategoryJpaRepository.existsById(itemCategoryId)) {
            throw new ResourceNotFoundException("Item category not found: " + itemCategoryId);
        }

        return DonationItem.builder()
                .donationId(donationId)
                .itemCategoryId(itemCategoryId)
                .build();
    }

    private String generateUniqueQrCodeToken() {
        for (int attempt = 0; attempt < 5; attempt++) {
            String token = "DON-" + UUID.randomUUID().toString().replace("-", "").toUpperCase(Locale.ROOT);
            if (!donationRepository.existsByQrCodeToken(token)) {
                return token;
            }
        }
        throw new IllegalStateException("Unable to generate unique donation QR token");
    }

    private String generateDonationCode() {
    String timePart = String.valueOf(System.currentTimeMillis()).substring(7);
    String randomPart = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
    return "DON-" + timePart + "-" + randomPart;
}
}
