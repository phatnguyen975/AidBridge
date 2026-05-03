package com.drc.aidbridge.modules.donation.internal.usecase;

import com.drc.aidbridge.modules.donation.internal.entity.Donation;
import com.drc.aidbridge.modules.donation.internal.repository.DonationRepository;
import com.drc.aidbridge.modules.donation.internal.web.dto.DonationQrResponse;
import com.drc.aidbridge.modules.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GetDonationQrUseCaseTest {

    private DonationRepository donationRepository;
    private GetDonationQrUseCase useCase;

    @BeforeEach
    void setUp() {
        donationRepository = mock(DonationRepository.class);
        useCase = new GetDonationQrUseCase(donationRepository);
    }

    @Test
    void execute_ShouldReturnQr_WhenDonationExists() {
        UUID donationId = UUID.randomUUID();
        String qrToken = "DON-QR-TOKEN";

        Donation donation = Donation.builder()
                .id(donationId)
                .qrCodeToken(qrToken)
                .build();

        when(donationRepository.findById(donationId)).thenReturn(Optional.of(donation));

        DonationQrResponse response = useCase.execute(donationId);

        assertEquals(donationId, response.getDonationId());
        assertEquals(qrToken, response.getQrCodeToken());
    }

    @Test
    void execute_ShouldThrow_WhenDonationNotFound() {
        UUID donationId = UUID.randomUUID();
        when(donationRepository.findById(donationId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> useCase.execute(donationId));
    }
}
