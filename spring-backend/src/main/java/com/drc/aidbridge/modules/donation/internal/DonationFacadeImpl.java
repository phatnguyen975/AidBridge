package com.drc.aidbridge.modules.donation.internal;

import com.drc.aidbridge.modules.donation.DonationDTO;
import com.drc.aidbridge.modules.donation.DonationFacade;
import com.drc.aidbridge.modules.donation.internal.usecase.CreateDonationUseCase;
import com.drc.aidbridge.modules.donation.internal.usecase.GetDonationByIdUseCase;
import com.drc.aidbridge.modules.donation.internal.web.dto.CreateDonationRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DonationFacadeImpl implements DonationFacade {

    private final CreateDonationUseCase createDonationUseCase;
    private final GetDonationByIdUseCase getDonationByIdUseCase;

    @Override
    public DonationDTO getById(UUID id) {
        return getDonationByIdUseCase.execute(id);
    }

    @Override
    public DonationDTO create(UUID sponsorId, CreateDonationRequest request) {
        return createDonationUseCase.execute(sponsorId, request);
    }
}
