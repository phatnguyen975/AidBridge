package com.drc.aidbridge.modules.donation.internal;

import com.drc.aidbridge.modules.donation.DonationDTO;
import com.drc.aidbridge.modules.donation.DonationFacade;
import com.drc.aidbridge.modules.donation.internal.usecase.GetDonationByIdUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DonationFacadeImpl implements DonationFacade {

    private final GetDonationByIdUseCase getDonationByIdUseCase;

    @Override
    public DonationDTO getById(UUID id) {
        return getDonationByIdUseCase.execute(id);
    }
}
