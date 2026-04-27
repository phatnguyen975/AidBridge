package com.drc.aidbridge.domain.usecase.sponsor;

import androidx.lifecycle.LiveData;
import androidx.annotation.Nullable;

import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.domain.model.sponsor.SponsorDonationHistoryPage;
import com.drc.aidbridge.domain.model.sponsor.SponsorDonationStatus;
import com.drc.aidbridge.domain.repository.sponsor.SponsorDonationRepository;

import javax.inject.Inject;

public class GetSponsorDonationHistoryUseCase {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_LIMIT = 10;

    private final SponsorDonationRepository sponsorDonationRepository;

    @Inject
    public GetSponsorDonationHistoryUseCase(SponsorDonationRepository sponsorDonationRepository) {
        this.sponsorDonationRepository = sponsorDonationRepository;
    }

    public LiveData<NetworkResultWrapper<SponsorDonationHistoryPage>> execute(SponsorDonationStatus status,
                                                                               int page,
                                                                               int limit) {
        int safePage = Math.max(DEFAULT_PAGE, page);
        int safeLimit = Math.max(1, limit);
        String statusValue = status != null ? status.name() : null;
        return sponsorDonationRepository.getDonationHistory(statusValue, safePage, safeLimit);
    }

    public LiveData<NetworkResultWrapper<SponsorDonationHistoryPage>> execute(@Nullable SponsorDonationStatus status) {
        return execute(status, DEFAULT_PAGE, DEFAULT_LIMIT);
    }
}
