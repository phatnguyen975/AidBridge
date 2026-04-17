package com.drc.aidbridge.modules.donation.internal.usecase;

import com.drc.aidbridge.modules.donation.DonationDTO;
import com.drc.aidbridge.modules.donation.internal.entity.Donation;
import com.drc.aidbridge.modules.donation.internal.mapper.DonationMapper;
import com.drc.aidbridge.modules.donation.internal.repository.DonationRepository;
import com.drc.aidbridge.modules.shared.dto.PaginatedResponseDto;
import com.drc.aidbridge.modules.shared.enums.DonationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ListDonationsUseCaseTest {

    private DonationRepository donationRepository;
    private DonationMapper donationMapper;
    private ListDonationsUseCase useCase;

    @BeforeEach
    void setUp() {
        donationRepository = mock(DonationRepository.class);
        donationMapper = mock(DonationMapper.class);
        useCase = new ListDonationsUseCase(donationRepository, donationMapper);
    }

    @Test
    void execute_shouldReturnPagedDonations_whenFilterByStatusAndHub() {
        UUID hubId = UUID.randomUUID();
        UUID donationId = UUID.randomUUID();

        Donation donation = Donation.builder()
                .id(donationId)
                .status(DonationStatus.REGISTERED)
                .hubId(hubId)
                .build();

        DonationDTO dto = DonationDTO.builder()
                .id(donationId)
                .status(DonationStatus.REGISTERED)
                .hubId(hubId)
                .build();

        when(donationRepository.findByStatusAndHubId(eq(DonationStatus.REGISTERED), eq(hubId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(donation), PageRequest.of(0, 20), 1));
        when(donationMapper.toDTO(donation)).thenReturn(dto);

        PaginatedResponseDto<DonationDTO> response = useCase.execute(DonationStatus.REGISTERED, hubId, 1, 20);

        assertEquals(1, response.getItems().size());
        assertEquals(donationId, response.getItems().getFirst().getId());
        assertEquals(1, response.getPagination().getPage());
        assertEquals(20, response.getPagination().getLimit());
        assertEquals(1L, response.getPagination().getTotal());
        assertEquals(1, response.getPagination().getTotalPages());
    }

    @Test
    void execute_shouldReturnPagedDonations_whenNoFilter() {
        UUID donationId = UUID.randomUUID();
        Donation donation = Donation.builder().id(donationId).status(DonationStatus.REGISTERED).build();
        DonationDTO dto = DonationDTO.builder().id(donationId).status(DonationStatus.REGISTERED).build();

        when(donationRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(donation), PageRequest.of(0, 10), 1));
        when(donationMapper.toDTO(donation)).thenReturn(dto);

        PaginatedResponseDto<DonationDTO> response = useCase.execute(null, null, 1, 10);

        assertEquals(1, response.getItems().size());
        assertEquals(donationId, response.getItems().getFirst().getId());
        assertEquals(false, response.getPagination().isHasNext());
    }

    @Test
    void execute_shouldThrow_whenPageOrLimitInvalid() {
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(null, null, 0, 10));
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(null, null, 1, 0));
    }
}
