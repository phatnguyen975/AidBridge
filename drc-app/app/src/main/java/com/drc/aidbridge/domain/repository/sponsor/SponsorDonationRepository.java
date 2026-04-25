package com.drc.aidbridge.domain.repository.sponsor;

import androidx.lifecycle.LiveData;

import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.domain.model.sponsor.SponsorDonationRequest;
import com.drc.aidbridge.domain.model.sponsor.SponsorDonationSubmissionResult;

public interface SponsorDonationRepository {

    LiveData<NetworkResultWrapper<SponsorDonationSubmissionResult>> submitDonation(SponsorDonationRequest request);
}
