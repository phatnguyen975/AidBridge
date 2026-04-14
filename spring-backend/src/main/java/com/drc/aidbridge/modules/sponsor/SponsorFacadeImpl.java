package com.drc.aidbridge.modules.sponsor;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.drc.aidbridge.modules.sponsor.internal.usecase.CreateSponsorProfileUseCase;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SponsorFacadeImpl implements SponsorFacade {

    private final CreateSponsorProfileUseCase createSponsorProfileUseCase;

    @Override
    public SponsorDTO createSponsorProfile(UUID userId) {
        return createSponsorProfileUseCase.execute(userId);
    }

}
