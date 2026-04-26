package com.drc.aidbridge.modules.donation.internal;

import com.drc.aidbridge.modules.donation.DonationDTO;
import com.drc.aidbridge.modules.donation.DonationFacade;
import com.drc.aidbridge.modules.donation.internal.usecase.CreateDonationUseCase;
import com.drc.aidbridge.modules.donation.internal.usecase.GetDonationByIdUseCase;
import com.drc.aidbridge.modules.donation.internal.usecase.GetDonationQrUseCase;
import com.drc.aidbridge.modules.donation.internal.usecase.ListDonationsUseCase;
import com.drc.aidbridge.modules.donation.internal.usecase.UpdateDonationStatusUseCase;
import com.drc.aidbridge.modules.donation.internal.web.dto.CreateDonationRequest;
import com.drc.aidbridge.modules.donation.internal.web.dto.DonationQrResponse;
import com.drc.aidbridge.modules.donation.internal.web.dto.UpdateDonationStatusRequest;
import com.drc.aidbridge.modules.shared.dto.PaginatedResponseDto;
import com.drc.aidbridge.modules.shared.enums.DonationStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DonationFacadeImpl implements DonationFacade {

    private final CreateDonationUseCase createDonationUseCase;
    private final GetDonationByIdUseCase getDonationByIdUseCase;
    private final GetDonationQrUseCase getDonationQrUseCase;
    private final ListDonationsUseCase listDonationsUseCase;
    private final UpdateDonationStatusUseCase updateDonationStatusUseCase;

    @Override
    public DonationDTO getById(UUID id) {
        return getDonationByIdUseCase.execute(id);
    }

    @Override
    public DonationQrResponse getQrById(UUID id) {
        return getDonationQrUseCase.execute(id);
    }

    @Override
    public DonationDTO create(UUID sponsorId, CreateDonationRequest request) {
        return createDonationUseCase.execute(sponsorId, request);
    }

    @Override
    public PaginatedResponseDto<DonationDTO> list(DonationStatus status, UUID hubId, int page, int limit) {
        return listDonationsUseCase.execute(status, hubId, page, limit);
    }

    @Override
    public PaginatedResponseDto<DonationDTO> listBySponsor(UUID sponsorId, DonationStatus status, int page, int limit) {
        return listDonationsUseCase.executeBySponsor(sponsorId, status, page, limit);
    }

    @Override
    public DonationDTO updateStatus(UUID id, UpdateDonationStatusRequest request) {
        return updateDonationStatusUseCase.execute(id, request);
    }
}
