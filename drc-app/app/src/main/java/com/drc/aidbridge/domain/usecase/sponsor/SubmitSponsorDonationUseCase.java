package com.drc.aidbridge.domain.usecase.sponsor;

import androidx.lifecycle.LiveData;

import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.domain.model.sponsor.SponsorDonationRequest;
import com.drc.aidbridge.domain.model.sponsor.SponsorDonationSubmissionResult;
import com.drc.aidbridge.domain.repository.sponsor.SponsorDonationRepository;
import com.drc.aidbridge.domain.usecase.validation.SponsorDonationInputValidator;
import com.drc.aidbridge.domain.usecase.validation.ValidationResult;

import javax.inject.Inject;

public class SubmitSponsorDonationUseCase {

    private final SponsorDonationRepository sponsorDonationRepository;
    private final SponsorDonationInputValidator sponsorDonationInputValidator;

    @Inject
    public SubmitSponsorDonationUseCase(SponsorDonationRepository sponsorDonationRepository,
                                        SponsorDonationInputValidator sponsorDonationInputValidator) {
        this.sponsorDonationRepository = sponsorDonationRepository;
        this.sponsorDonationInputValidator = sponsorDonationInputValidator;
    }

    public ValidationResult validate(SponsorDonationRequest request) {
        return sponsorDonationInputValidator.validateDonationRequest(request);
    }

    public LiveData<NetworkResultWrapper<SponsorDonationSubmissionResult>> execute(SponsorDonationRequest request) {
        return sponsorDonationRepository.submitDonation(request);
    }
}
