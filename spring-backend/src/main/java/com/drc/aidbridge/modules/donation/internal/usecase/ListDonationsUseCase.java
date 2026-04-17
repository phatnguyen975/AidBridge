package com.drc.aidbridge.modules.donation.internal.usecase;

import com.drc.aidbridge.modules.donation.DonationDTO;
import com.drc.aidbridge.modules.donation.internal.entity.Donation;
import com.drc.aidbridge.modules.donation.internal.mapper.DonationMapper;
import com.drc.aidbridge.modules.donation.internal.repository.DonationRepository;
import com.drc.aidbridge.modules.shared.dto.PaginatedResponseDto;
import com.drc.aidbridge.modules.shared.dto.PaginationDto;
import com.drc.aidbridge.modules.shared.enums.DonationStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ListDonationsUseCase {

    private static final int MAX_LIMIT = 100;

    private final DonationRepository donationRepository;
    private final DonationMapper donationMapper;

    public PaginatedResponseDto<DonationDTO> execute(DonationStatus status, UUID hubId, int page, int limit) {
        validatePagination(page, limit);

        int safeLimit = Math.min(limit, MAX_LIMIT);
        Pageable pageable = PageRequest.of(page - 1, safeLimit, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Donation> donationPage = findPage(status, hubId, pageable);
        List<DonationDTO> items = donationPage.getContent().stream()
                .map(donationMapper::toDTO)
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

    private void validatePagination(int page, int limit) {
        if (page < 1) {
            throw new IllegalArgumentException("page must be greater than or equal to 1");
        }
        if (limit < 1) {
            throw new IllegalArgumentException("limit must be greater than or equal to 1");
        }
    }
}
