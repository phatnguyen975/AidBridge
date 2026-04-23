package com.drc.aidbridge.domain.repository.sponsor;

import androidx.lifecycle.LiveData;

import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.domain.model.sponsor.SponsorDonationRequest;

public interface SponsorDonationRepository {

    LiveData<NetworkResultWrapper<String>> submitDonation(SponsorDonationRequest request);
}
