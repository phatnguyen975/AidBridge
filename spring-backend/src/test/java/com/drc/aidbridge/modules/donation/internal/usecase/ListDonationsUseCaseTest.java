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
import com.drc.aidbridge.modules.shared.enums.DonationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ListDonationsUseCaseTest {

    private DonationRepository donationRepository;
    private DonationItemRepository donationItemRepository;
    private HubRepository hubRepository;
    private DonationMapper donationMapper;
    private ListDonationsUseCase useCase;

    @BeforeEach
    void setUp() {
        donationRepository = mock(DonationRepository.class);
        donationItemRepository = mock(DonationItemRepository.class);
        hubRepository = mock(HubRepository.class);
        donationMapper = mock(DonationMapper.class);
        useCase = new ListDonationsUseCase(donationRepository, donationItemRepository, hubRepository, donationMapper);
    }

    @Test
    void executeBySponsor_shouldReturnHistorySummaryWithAggregates() {
        UUID sponsorId = UUID.randomUUID();
        UUID donationId = UUID.randomUUID();
        UUID hubId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-04-27T10:15:30Z");

        Donation donation = Donation.builder()
                .id(donationId)
                .hubId(hubId)
                .donationCode("DN-20260427-001")
                .status(DonationStatus.REGISTERED)
                .createdAt(createdAt)
                .qrCodeToken("abc123xyz")
                .build();

        when(donationRepository.findBySponsorIdAndStatus(eq(sponsorId), eq(DonationStatus.REGISTERED), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(donation), PageRequest.of(0, 10), 1));

        DonationItemRepository.DonationHistoryAggregateProjection projection =
                mock(DonationItemRepository.DonationHistoryAggregateProjection.class);
        when(projection.getDonationId()).thenReturn(donationId);
        when(projection.getItemSummary()).thenReturn("Quần áo, Nước uống");
        when(projection.getTotalQuantity()).thenReturn(2L);
        when(donationItemRepository.findHistoryAggregatesByDonationIds(List.of(donationId)))
                .thenReturn(List.of(projection));

        Hub hub = Hub.builder().id(hubId).name("Trung tâm cứu trợ Quận 1").build();
        when(hubRepository.findAllById(List.of(hubId))).thenReturn(List.of(hub));

        PaginatedResponseDto<DonationHistorySummaryDTO> response =
                useCase.executeBySponsor(sponsorId, DonationStatus.REGISTERED, 1, 10);

        assertEquals(1, response.getItems().size());
        DonationHistorySummaryDTO item = response.getItems().getFirst();
        assertEquals(donationId, item.getId());
        assertEquals("DN-20260427-001", item.getDonationCode());
        assertEquals("Trung tâm cứu trợ Quận 1", item.getHubName());
        assertEquals("Quần áo, Nước uống", item.getItemSummary());
        assertEquals(2, item.getTotalQuantity());
        assertEquals(DonationStatus.REGISTERED, item.getStatus());
        assertEquals(createdAt, item.getCreatedAt());
        assertEquals("abc123xyz", item.getQrCodeToken());

        verify(donationItemRepository).findHistoryAggregatesByDonationIds(List.of(donationId));
        verify(hubRepository).findAllById(List.of(hubId));
    }

    @Test
    void executeBySponsor_shouldThrow_whenSponsorIdIsNull() {
        assertThrows(IllegalArgumentException.class,
                () -> useCase.executeBySponsor(null, DonationStatus.REGISTERED, 1, 10));
    }

    @Test
    void execute_shouldStillReturnDonationDtoList() {
        UUID donationId = UUID.randomUUID();
        Donation donation = Donation.builder().id(donationId).build();
        DonationDTO dto = DonationDTO.builder().id(donationId).build();

        when(donationRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(donation), PageRequest.of(0, 10), 1));
        when(donationMapper.toDTO(donation)).thenReturn(dto);
        when(donationItemRepository.findAllByDonationId(donationId)).thenReturn(List.of());

        PaginatedResponseDto<DonationDTO> response = useCase.execute(null, null, 1, 10);

        assertEquals(1, response.getItems().size());
        assertEquals(donationId, response.getItems().getFirst().getId());
    }
}
