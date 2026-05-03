package com.drc.aidbridge.ui.main.viewmodel.victim;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.domain.model.victim.VictimReliefRequest;
import com.drc.aidbridge.domain.model.victim.VictimRequestedItem;
import com.drc.aidbridge.domain.model.victim.VictimSupplyCategory;
import com.drc.aidbridge.domain.model.victim.VictimVoiceReliefRequest;
import com.drc.aidbridge.domain.usecase.validation.ValidationResult;
import com.drc.aidbridge.domain.usecase.victim.GetSupplyCategoriesUseCase;
import com.drc.aidbridge.domain.usecase.victim.SubmitReliefRequestUseCase;
import com.drc.aidbridge.domain.usecase.victim.SubmitVoiceReliefRequestUseCase;
import com.drc.aidbridge.ui.base.BaseViewModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class VictimSupplyViewModel extends BaseViewModel {

    private static final String DEFAULT_URGENCY_LEVEL = "MEDIUM";

    private final ExecutorService workerExecutor = Executors.newSingleThreadExecutor();
    private final GetSupplyCategoriesUseCase getSupplyCategoriesUseCase;
    private final SubmitReliefRequestUseCase submitReliefRequestUseCase;
    private final SubmitVoiceReliefRequestUseCase submitVoiceReliefRequestUseCase;

    private final MutableLiveData<Long> loadCategoriesTrigger = new MutableLiveData<>();
    private final MutableLiveData<VictimReliefRequest> submitRequestTrigger = new MutableLiveData<>();
    private final MutableLiveData<VictimVoiceReliefRequest> submitVoiceRequestTrigger = new MutableLiveData<>();

    private final LiveData<NetworkResultWrapper<List<VictimSupplyCategory>>> categoriesSource;
    private final LiveData<NetworkResultWrapper<String>> submitSource;
    private final LiveData<NetworkResultWrapper<String>> submitVoiceSource;

    private final MutableLiveData<ValidationResult> validationError = new MutableLiveData<>();
    private final MediatorLiveData<NetworkResultWrapper<List<VictimSupplyCategory>>> categoriesResult = new MediatorLiveData<>();
    private final MediatorLiveData<NetworkResultWrapper<String>> submitResult = new MediatorLiveData<>();
    private final MediatorLiveData<NetworkResultWrapper<String>> submitVoiceResult = new MediatorLiveData<>();

    @Inject
    public VictimSupplyViewModel(GetSupplyCategoriesUseCase getSupplyCategoriesUseCase,
                                 SubmitReliefRequestUseCase submitReliefRequestUseCase,
                                 SubmitVoiceReliefRequestUseCase submitVoiceReliefRequestUseCase) {
        this.getSupplyCategoriesUseCase = getSupplyCategoriesUseCase;
        this.submitReliefRequestUseCase = submitReliefRequestUseCase;
        this.submitVoiceReliefRequestUseCase = submitVoiceReliefRequestUseCase;

        categoriesSource = Transformations.switchMap(loadCategoriesTrigger,
            ignored -> this.getSupplyCategoriesUseCase.execute());

        submitSource = Transformations.switchMap(submitRequestTrigger,
            this.submitReliefRequestUseCase::execute);
        submitVoiceSource = Transformations.switchMap(submitVoiceRequestTrigger,
            this.submitVoiceReliefRequestUseCase::execute);

        categoriesResult.addSource(categoriesSource, categoriesResult::postValue);
        submitResult.addSource(submitSource, submitResult::postValue);
        submitVoiceResult.addSource(submitVoiceSource, submitVoiceResult::postValue);

        loadCategories();
    }

    public LiveData<NetworkResultWrapper<List<VictimSupplyCategory>>> getCategoriesResult() {
        return categoriesResult;
    }

    public LiveData<NetworkResultWrapper<String>> getSubmitResult() {
        return submitResult;
    }

    public LiveData<NetworkResultWrapper<String>> getVoiceSubmitResult() {
        return submitVoiceResult;
    }

    public LiveData<ValidationResult> getValidationError() {
        return validationError;
    }

    public void loadCategories() {
        workerExecutor.execute(() -> loadCategoriesTrigger.postValue(System.currentTimeMillis()));
    }

    public void submitRequest(int adults,
                              int elders,
                              int children,
                              String note,
                              List<RequestedItem> items,
                              Double latitude,
                              Double longitude) {
        workerExecutor.execute(() -> {
            List<VictimRequestedItem> requestedItems = mapRequestedItems(items);
            VictimReliefRequest request = new VictimReliefRequest(
                latitude,
                longitude,
                "",
                Math.max(0, adults),
                Math.max(0, elders),
                Math.max(0, children),
                note != null ? note.trim() : "",
                DEFAULT_URGENCY_LEVEL,
                requestedItems
            );

            ValidationResult validation = submitReliefRequestUseCase.validate(request);
            if (!validation.isValid()) {
                validationError.postValue(validation);
                return;
            }

            validationError.postValue(ValidationResult.valid());
            submitRequestTrigger.postValue(request);
        });
    }

    public void submitVoiceRequest(int adults,
                                   int elders,
                                   int children,
                                   String note,
                                   Double latitude,
                                   Double longitude,
                                   java.io.File audioFile) {
        workerExecutor.execute(() -> {
            VictimVoiceReliefRequest request = new VictimVoiceReliefRequest(
                audioFile,
                latitude,
                longitude,
                "",
                Math.max(0, adults),
                Math.max(0, elders),
                Math.max(0, children),
                note != null ? note.trim() : "",
                DEFAULT_URGENCY_LEVEL
            );

            submitVoiceRequestTrigger.postValue(request);
        });
    }

    @Override
    protected void onCleared() {
        workerExecutor.shutdownNow();
        super.onCleared();
    }

    private List<VictimRequestedItem> mapRequestedItems(List<RequestedItem> items) {
        if (items == null || items.isEmpty()) {
            return Collections.emptyList();
        }

        List<VictimRequestedItem> result = new ArrayList<>();
        for (RequestedItem item : items) {
            if (item == null) {
                continue;
            }

            String itemId = item.getItemId();
            int quantity = item.getQuantity();
            if (itemId == null || itemId.trim().isEmpty() || quantity <= 0) {
                continue;
            }

            result.add(new VictimRequestedItem(itemId.trim(), quantity));
        }
        return result;
    }

    public static final class RequestedItem {
        private final String itemId;
        private final int quantity;

        public RequestedItem(String itemId, int quantity) {
            this.itemId = itemId;
            this.quantity = quantity;
        }

        public String getItemId() {
            return itemId;
        }

        public int getQuantity() {
            return quantity;
        }
    }
}
