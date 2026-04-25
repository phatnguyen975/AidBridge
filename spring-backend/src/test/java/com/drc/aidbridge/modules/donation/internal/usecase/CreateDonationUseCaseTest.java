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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CreateDonationUseCaseTest {

    private DonationRepository donationRepository;
    private DonationItemRepository donationItemRepository;
    private UserJpaRepository userJpaRepository;
    private HubRepository hubRepository;
    private AidItemCategoryJpaRepository aidItemCategoryJpaRepository;
    private DonationMapper donationMapper;
    private CreateDonationUseCase useCase;

    @BeforeEach
    void setUp() {
        donationRepository = mock(DonationRepository.class);
        donationItemRepository = mock(DonationItemRepository.class);
        userJpaRepository = mock(UserJpaRepository.class);
        hubRepository = mock(HubRepository.class);
        aidItemCategoryJpaRepository = mock(AidItemCategoryJpaRepository.class);
        donationMapper = mock(DonationMapper.class);

        useCase = new CreateDonationUseCase(
                donationRepository,
                donationItemRepository,
                userJpaRepository,
                hubRepository,
                aidItemCategoryJpaRepository,
                donationMapper
        );
    }

    @Test
    void execute_ShouldCreateDonationWithItems_WhenRequestValid() {
        UUID sponsorId = UUID.randomUUID();
        UUID hubId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        UUID donationId = UUID.randomUUID();

        CreateDonationRequest request = CreateDonationRequest.builder()
                .hubId(hubId)
                .items(List.of(CreateDonationItemRequest.builder()
                        .itemCategoryId(categoryId)
                        .build()))
                .build();

        Donation savedDonation = Donation.builder()
                .id(donationId)
                .sponsorId(sponsorId)
                .hubId(hubId)
                .status(DonationStatus.REGISTERED)
                .build();

        DonationItem savedItem = DonationItem.builder()
                .id(UUID.randomUUID())
                .donationId(donationId)
                .itemCategoryId(categoryId)
                .build();

        DonationDTO donationDTO = DonationDTO.builder()
                .id(donationId)
                .sponsorId(sponsorId)
                .hubId(hubId)
                .qrCodeToken("DON-TEST-QR")
                .status(DonationStatus.REGISTERED)
                .build();

        DonationDTO.DonationItemDTO itemDTO = DonationDTO.DonationItemDTO.builder()
                .id(savedItem.getId())
                .itemCategoryId(categoryId)
                .build();

        when(userJpaRepository.existsById(sponsorId)).thenReturn(true);
        when(hubRepository.existsById(hubId)).thenReturn(true);
        when(aidItemCategoryJpaRepository.existsById(categoryId)).thenReturn(true);
        when(donationRepository.existsByQrCodeToken(anyString())).thenReturn(false);
        when(donationRepository.save(any(Donation.class))).thenReturn(savedDonation);
        when(donationItemRepository.saveAll(anyList())).thenReturn(List.of(savedItem));
        when(donationMapper.toDTO(savedDonation)).thenReturn(donationDTO);
        when(donationMapper.toItemDTO(savedItem)).thenReturn(itemDTO);

        DonationDTO result = useCase.execute(sponsorId, request);

        ArgumentCaptor<Donation> donationCaptor = ArgumentCaptor.forClass(Donation.class);
        verify(donationRepository).save(donationCaptor.capture());
        String generatedToken = donationCaptor.getValue().getQrCodeToken();

        assertEquals(donationId, result.getId());
        assertEquals(DonationStatus.REGISTERED, result.getStatus());
        assertEquals(1, result.getItems().size());
        assertEquals(categoryId, result.getItems().getFirst().getItemCategoryId());
        assertNotNull(generatedToken);
        assertFalse(generatedToken.isBlank());
    }

    @Test
    void execute_ShouldThrow_WhenHubNotFound() {
        UUID sponsorId = UUID.randomUUID();
        UUID hubId = UUID.randomUUID();

        CreateDonationRequest request = CreateDonationRequest.builder()
                .hubId(hubId)
                .items(List.of(CreateDonationItemRequest.builder()
                        .itemCategoryId(UUID.randomUUID())
                        .build()))
                .build();

        when(userJpaRepository.existsById(sponsorId)).thenReturn(true);
        when(hubRepository.existsById(hubId)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> useCase.execute(sponsorId, request));
    }
}
