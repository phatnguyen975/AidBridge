package com.drc.aidbridge.modules.donation;

import java.util.UUID;

public interface DonationFacade {
    DonationDTO getById(UUID id);
}
