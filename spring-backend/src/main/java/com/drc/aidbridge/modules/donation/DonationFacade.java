package com.drc.aidbridge.modules.donation;

import com.drc.aidbridge.modules.donation.internal.web.dto.CreateDonationRequest;

import java.util.UUID;

public interface DonationFacade {
    DonationDTO getById(UUID id);
    DonationDTO create(UUID sponsorId, CreateDonationRequest request);
}
