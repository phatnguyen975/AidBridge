package com.drc.aidbridge.ui.main.viewmodel.sponsor;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.domain.model.admin.Hub;
import com.drc.aidbridge.domain.model.sponsor.SponsorDonationRequest;
import com.drc.aidbridge.domain.usecase.admin.ListHubsUseCase;
import com.drc.aidbridge.domain.usecase.sponsor.SubmitSponsorDonationUseCase;
import com.drc.aidbridge.domain.usecase.validation.ValidationResult;
import com.drc.aidbridge.ui.base.BaseViewModel;

import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class SponsorDonateViewModel extends BaseViewModel {

    private final SubmitSponsorDonationUseCase submitSponsorDonationUseCase;
    private final MutableLiveData<ValidationResult> validationError = new MutableLiveData<>();
    private final MutableLiveData<Long> loadHubsTrigger = new MutableLiveData<>();
    private final MutableLiveData<SponsorDonationRequest> submitTrigger = new MutableLiveData<>();

    private final LiveData<NetworkResultWrapper<List<Hub>>> hubsResult;
    private final LiveData<NetworkResultWrapper<String>> submitResult;

    @Inject
    public SponsorDonateViewModel(SubmitSponsorDonationUseCase submitSponsorDonationUseCase,
                                  ListHubsUseCase listHubsUseCase) {
        this.submitSponsorDonationUseCase = submitSponsorDonationUseCase;

        this.hubsResult = Transformations.switchMap(
            loadHubsTrigger,
            ignored -> listHubsUseCase.execute()
        );

        this.submitResult = Transformations.switchMap(
            submitTrigger,
            this.submitSponsorDonationUseCase::execute
        );
    }

    public LiveData<ValidationResult> getValidationError() {
        return validationError;
    }

    public LiveData<NetworkResultWrapper<List<Hub>>> getHubsResult() {
        return hubsResult;
    }

    public LiveData<NetworkResultWrapper<String>> getSubmitResult() {
        return submitResult;
    }

    public void loadAvailableHubs() {
        loadHubsTrigger.setValue(System.currentTimeMillis());
    }

    public void submitDonation(String hubId,
                               String category,
                               String itemName,
                               String quantityRaw,
                               String unit,
                               String description,
                               String expectedTime) {
        int quantity = parseQuantity(quantityRaw);

        SponsorDonationRequest request = new SponsorDonationRequest(
            safeText(hubId),
            safeText(category),
            safeText(itemName),
            quantity,
            safeText(unit),
            safeText(description),
            safeText(expectedTime),
            null
        );

        ValidationResult validation = submitSponsorDonationUseCase.validate(request);
        if (!validation.isValid()) {
            validationError.setValue(validation);
            return;
        }

        validationError.setValue(ValidationResult.valid());
        submitTrigger.setValue(request);
    }

    private int parseQuantity(String quantityRaw) {
        if (quantityRaw == null || quantityRaw.trim().isEmpty()) {
            return 0;
        }

        try {
            return Integer.parseInt(quantityRaw.trim());
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    private String safeText(String value) {
        return value != null ? value.trim() : "";
    }
}
