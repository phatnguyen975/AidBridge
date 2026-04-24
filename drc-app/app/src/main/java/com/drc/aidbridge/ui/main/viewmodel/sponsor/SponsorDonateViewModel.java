package com.drc.aidbridge.ui.main.viewmodel.sponsor;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.domain.model.admin.Hub;
import com.drc.aidbridge.domain.model.sponsor.SponsorDonationItem;
import com.drc.aidbridge.domain.model.sponsor.SponsorDonationRequest;
import com.drc.aidbridge.domain.model.victim.VictimSupplyCategory;
import com.drc.aidbridge.domain.usecase.admin.ListHubsUseCase;
import com.drc.aidbridge.domain.usecase.sponsor.SubmitSponsorDonationUseCase;
import com.drc.aidbridge.domain.usecase.victim.GetSupplyCategoriesUseCase;
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
    private final MutableLiveData<Long> loadCategoriesTrigger = new MutableLiveData<>();
    private final MutableLiveData<SponsorDonationRequest> submitTrigger = new MutableLiveData<>();

    private final LiveData<NetworkResultWrapper<List<Hub>>> hubsResult;
    private final LiveData<NetworkResultWrapper<List<VictimSupplyCategory>>> categoriesResult;
    private final LiveData<NetworkResultWrapper<String>> submitResult;

    @Inject
    public SponsorDonateViewModel(SubmitSponsorDonationUseCase submitSponsorDonationUseCase,
                                  ListHubsUseCase listHubsUseCase,
                                  GetSupplyCategoriesUseCase getSupplyCategoriesUseCase) {
        this.submitSponsorDonationUseCase = submitSponsorDonationUseCase;

        this.hubsResult = Transformations.switchMap(
            loadHubsTrigger,
            ignored -> listHubsUseCase.execute()
        );

        this.categoriesResult = Transformations.switchMap(
            loadCategoriesTrigger,
            ignored -> getSupplyCategoriesUseCase.execute()
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

    public LiveData<NetworkResultWrapper<List<VictimSupplyCategory>>> getCategoriesResult() {
        return categoriesResult;
    }

    public LiveData<NetworkResultWrapper<String>> getSubmitResult() {
        return submitResult;
    }

    public void loadAvailableHubs() {
        loadHubsTrigger.setValue(System.currentTimeMillis());
    }

    public void loadParentCategories() {
        loadCategoriesTrigger.setValue(System.currentTimeMillis());
    }

    public void submitDonation(String hubId,
                               String notes,
                               List<SponsorDonationItem> items) {

        SponsorDonationRequest request = new SponsorDonationRequest(
            safeText(hubId),
            safeText(notes),
            items
        );

        ValidationResult validation = submitSponsorDonationUseCase.validate(request);
        if (!validation.isValid()) {
            validationError.setValue(validation);
            return;
        }

        validationError.setValue(ValidationResult.valid());
        submitTrigger.setValue(request);
    }

    private String safeText(String value) {
        return value != null ? value.trim() : "";
    }
}
