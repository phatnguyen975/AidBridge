package com.drc.aidbridge.modules.donation;

import com.drc.aidbridge.modules.donation.internal.web.dto.CreateDonationRequest;
import com.drc.aidbridge.modules.donation.internal.web.dto.DonationQrResponse;
import com.drc.aidbridge.modules.donation.internal.web.dto.UpdateDonationStatusRequest;
import com.drc.aidbridge.modules.shared.dto.PaginatedResponseDto;
import com.drc.aidbridge.modules.shared.enums.DonationStatus;

import java.util.UUID;

public interface DonationFacade {
    DonationDTO getById(UUID id);
    DonationQrResponse getQrById(UUID id);
    DonationDTO create(UUID sponsorId, CreateDonationRequest request);
    PaginatedResponseDto<DonationDTO> list(DonationStatus status, UUID hubId, int page, int limit);
    DonationDTO updateStatus(UUID id, UpdateDonationStatusRequest request);
}
