package com.drc.aidbridge.modules.donation.internal.usecase;

import com.drc.aidbridge.modules.donation.DonationDTO;
import com.drc.aidbridge.modules.donation.internal.entity.Donation;
import com.drc.aidbridge.modules.donation.internal.entity.DonationItem;
import com.drc.aidbridge.modules.donation.internal.mapper.DonationMapper;
import com.drc.aidbridge.modules.donation.internal.repository.DonationItemRepository;
import com.drc.aidbridge.modules.donation.internal.repository.DonationRepository;
import com.drc.aidbridge.modules.donation.internal.web.dto.UpdateDonationStatusRequest;
import com.drc.aidbridge.modules.shared.enums.DonationStatus;
import com.drc.aidbridge.modules.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UpdateDonationStatusUseCaseTest {

    private DonationRepository donationRepository;
    private DonationItemRepository donationItemRepository;
    private DonationMapper donationMapper;
    private UpdateDonationStatusUseCase useCase;

    @BeforeEach
    void setUp() {
        donationRepository = mock(DonationRepository.class);
        donationItemRepository = mock(DonationItemRepository.class);
        donationMapper = mock(DonationMapper.class);
        useCase = new UpdateDonationStatusUseCase(donationRepository, donationItemRepository, donationMapper);
    }

    @Test
    void execute_shouldUpdateStatusAndReturnDonation() {
        UUID donationId = UUID.randomUUID();
        Donation donation = Donation.builder()
                .id(donationId)
                .status(DonationStatus.REGISTERED)
                .notes("old")
                .build();

        UpdateDonationStatusRequest request = UpdateDonationStatusRequest.builder()
                .status(DonationStatus.READY_FOR_INBOUND)
                .notes("reviewed")
                .build();

        DonationDTO dto = DonationDTO.builder()
                .id(donationId)
                .status(DonationStatus.READY_FOR_INBOUND)
                .notes("reviewed")
                .build();

        DonationItem item = DonationItem.builder()
                .id(UUID.randomUUID())
                .donationId(donationId)
                .itemName("Blanket")
                .build();

        DonationDTO.DonationItemDTO itemDTO = DonationDTO.DonationItemDTO.builder()
                .id(item.getId())
                .itemName("Blanket")
                .build();

        when(donationRepository.findById(donationId)).thenReturn(Optional.of(donation));
        when(donationRepository.save(any(Donation.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(donationMapper.toDTO(any(Donation.class))).thenReturn(dto);
        when(donationItemRepository.findAllByDonationId(donationId)).thenReturn(List.of(item));
        when(donationMapper.toItemDTO(item)).thenReturn(itemDTO);

        DonationDTO result = useCase.execute(donationId, request);

        assertEquals(DonationStatus.READY_FOR_INBOUND, result.getStatus());
        assertEquals("reviewed", result.getNotes());
        assertEquals(1, result.getItems().size());
    }

    @Test
    void execute_shouldThrowWhenDonationNotFound() {
        UUID donationId = UUID.randomUUID();
        UpdateDonationStatusRequest request = UpdateDonationStatusRequest.builder()
                .status(DonationStatus.COMPLETED)
                .build();

        when(donationRepository.findById(donationId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> useCase.execute(donationId, request));
    }
}
