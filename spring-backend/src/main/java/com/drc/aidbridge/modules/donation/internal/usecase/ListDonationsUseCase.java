package com.drc.aidbridge.modules.donation.internal.usecase;

import com.drc.aidbridge.modules.donation.DonationDTO;
import com.drc.aidbridge.modules.donation.DonationHistorySummaryDTO;
import com.drc.aidbridge.modules.donation.internal.entity.Donation;
import com.drc.aidbridge.modules.donation.internal.mapper.DonationMapper;
import com.drc.aidbridge.modules.donation.internal.repository.DonationItemRepository;
import com.drc.aidbridge.modules.donation.internal.repository.DonationRepository;
import com.drc.aidbridge.modules.hub.internal.entity.Hub;
import com.drc.aidbridge.modules.hub.internal.repository.HubRepository;
import com.drc.aidbridge.modules.shared.dto.PaginatedResponseDto;
import com.drc.aidbridge.modules.shared.dto.PaginationDto;
import com.drc.aidbridge.modules.shared.enums.DonationStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ListDonationsUseCase {

    private static final int MAX_LIMIT = 100;

    private final DonationRepository donationRepository;
    private final DonationItemRepository donationItemRepository;
    private final HubRepository hubRepository;
    private final DonationMapper donationMapper;

    public PaginatedResponseDto<DonationDTO> execute(DonationStatus status, UUID hubId, int page, int limit) {
        validatePagination(page, limit);

        int safeLimit = Math.min(limit, MAX_LIMIT);
        Pageable pageable = PageRequest.of(page - 1, safeLimit, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Donation> donationPage = findPage(status, hubId, pageable);
        List<DonationDTO> items = donationPage.getContent().stream()
            .map(this::toDonationDTOWithItems)
                .toList();

        PaginationDto pagination = PaginationDto.builder()
                .page(page)
                .limit(safeLimit)
                .total(donationPage.getTotalElements())
                .totalPages(donationPage.getTotalPages())
                .hasNext(donationPage.hasNext())
                .build();

        return PaginatedResponseDto.<DonationDTO>builder()
                .items(items)
                .pagination(pagination)
                .build();
    }

    public PaginatedResponseDto<DonationHistorySummaryDTO> executeBySponsor(UUID sponsorId, DonationStatus status, int page, int limit) {
        if (sponsorId == null) {
            throw new IllegalArgumentException("sponsorId is required");
        }
        validatePagination(page, limit);

        int safeLimit = Math.min(limit, MAX_LIMIT);
        Pageable pageable = PageRequest.of(page - 1, safeLimit, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Donation> donationPage = findPageBySponsor(sponsorId, status, pageable);
        List<Donation> donations = donationPage.getContent();
        Map<UUID, DonationItemRepository.DonationHistoryAggregateProjection> aggregateByDonationId =
            loadHistoryAggregates(donations);
        Map<UUID, String> hubNameById = loadHubNames(donations);

        List<DonationHistorySummaryDTO> items = donations.stream()
            .map(donation -> toHistorySummaryDTO(
                donation,
                aggregateByDonationId.get(donation.getId()),
                hubNameById.get(donation.getHubId())
            ))
                .toList();

        PaginationDto pagination = PaginationDto.builder()
                .page(page)
                .limit(safeLimit)
                .total(donationPage.getTotalElements())
                .totalPages(donationPage.getTotalPages())
                .hasNext(donationPage.hasNext())
                .build();

        return PaginatedResponseDto.<DonationHistorySummaryDTO>builder()
                .items(items)
                .pagination(pagination)
                .build();
    }

    private DonationHistorySummaryDTO toHistorySummaryDTO(
            Donation donation,
            DonationItemRepository.DonationHistoryAggregateProjection aggregate,
            String hubName
    ) {
        long totalQuantity = aggregate != null && aggregate.getTotalQuantity() != null
                ? aggregate.getTotalQuantity()
                : 0L;

        int safeTotalQuantity = totalQuantity > Integer.MAX_VALUE
                ? Integer.MAX_VALUE
                : (int) Math.max(totalQuantity, 0L);

        return DonationHistorySummaryDTO.builder()
                .id(donation.getId())
                .donationCode(donation.getDonationCode())
                .hubName(hubName != null ? hubName.trim() : "")
                .itemSummary(aggregate != null && aggregate.getItemSummary() != null ? aggregate.getItemSummary() : "")
                .totalQuantity(safeTotalQuantity)
                .status(donation.getStatus())
                .createdAt(donation.getCreatedAt())
                .qrCodeToken(donation.getQrCodeToken())
                .build();
    }

    private Map<UUID, DonationItemRepository.DonationHistoryAggregateProjection> loadHistoryAggregates(List<Donation> donations) {
        if (donations == null || donations.isEmpty()) {
            return Collections.emptyMap();
        }

        List<UUID> donationIds = donations.stream()
                .map(Donation::getId)
                .filter(id -> id != null)
                .toList();

        if (donationIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<DonationItemRepository.DonationHistoryAggregateProjection> aggregates =
                donationItemRepository.findHistoryAggregatesByDonationIds(donationIds);

        Map<UUID, DonationItemRepository.DonationHistoryAggregateProjection> result = new HashMap<>();
        for (DonationItemRepository.DonationHistoryAggregateProjection aggregate : aggregates) {
            if (aggregate == null || aggregate.getDonationId() == null) {
                continue;
            }
            result.put(aggregate.getDonationId(), aggregate);
        }

        return result;
    }

    private Map<UUID, String> loadHubNames(List<Donation> donations) {
        if (donations == null || donations.isEmpty()) {
            return Collections.emptyMap();
        }

        List<UUID> hubIds = donations.stream()
                .map(Donation::getHubId)
                .filter(id -> id != null)
                .distinct()
                .toList();

        if (hubIds.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<UUID, String> result = new HashMap<>();
        List<Hub> hubs = hubRepository.findAllById(hubIds);
        for (Hub hub : hubs) {
            if (hub == null || hub.getId() == null) {
                continue;
            }
            result.put(hub.getId(), hub.getName());
        }

        return result;
    }

    private DonationDTO toDonationDTOWithItems(Donation donation) {
        DonationDTO donationDTO = donationMapper.toDTO(donation);
        donationDTO.setItems(
                donationItemRepository.findAllByDonationId(donation.getId()).stream()
                        .map(donationMapper::toItemDTO)
                        .toList()
        );
        return donationDTO;
    }

    private Page<Donation> findPage(DonationStatus status, UUID hubId, Pageable pageable) {
        if (status != null && hubId != null) {
            return donationRepository.findByStatusAndHubId(status, hubId, pageable);
        }
        if (status != null) {
            return donationRepository.findByStatus(status, pageable);
        }
        if (hubId != null) {
            return donationRepository.findByHubId(hubId, pageable);
        }
        return donationRepository.findAll(pageable);
    }

    private Page<Donation> findPageBySponsor(UUID sponsorId, DonationStatus status, Pageable pageable) {
        if (status != null) {
            return donationRepository.findBySponsorIdAndStatus(sponsorId, status, pageable);
        }
        return donationRepository.findBySponsorId(sponsorId, pageable);
    }

    private void validatePagination(int page, int limit) {
        if (page < 1) {
            throw new IllegalArgumentException("page must be greater than or equal to 1");
        }
        if (limit < 1) {
            throw new IllegalArgumentException("limit must be greater than or equal to 1");
        }
    }
}
