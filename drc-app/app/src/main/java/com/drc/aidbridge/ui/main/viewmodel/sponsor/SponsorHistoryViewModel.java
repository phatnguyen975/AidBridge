package com.drc.aidbridge.ui.main.viewmodel.sponsor;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.domain.model.sponsor.SponsorDonationHistoryItem;
import com.drc.aidbridge.domain.model.sponsor.SponsorDonationHistoryPage;
import com.drc.aidbridge.domain.model.sponsor.SponsorDonationStatus;
import com.drc.aidbridge.domain.usecase.sponsor.GetSponsorDonationHistoryUseCase;
import com.drc.aidbridge.ui.base.BaseViewModel;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class SponsorHistoryViewModel extends BaseViewModel {

    private static final int DEFAULT_PAGE = 1;
    private static final int PAGE_LIMIT = 10;

    private final MutableLiveData<HistoryRequest> historyTrigger = new MutableLiveData<>();
    private final LiveData<NetworkResultWrapper<SponsorDonationHistoryPage>> historyResult;
    private final MediatorLiveData<List<SponsorDonationHistoryItem>> historyItems = new MediatorLiveData<>();

    private final List<SponsorDonationHistoryItem> cachedHistoryItems = new ArrayList<>();

    @Nullable
    private SponsorDonationStatus selectedStatus = null;
    private int currentPage = DEFAULT_PAGE;
    private boolean isLoading;
    private boolean isLastPage;

    @Inject
    public SponsorHistoryViewModel(GetSponsorDonationHistoryUseCase getSponsorDonationHistoryUseCase) {
        this.historyResult = Transformations.switchMap(
                historyTrigger,
                request -> getSponsorDonationHistoryUseCase.execute(request.status, request.page, request.limit)
        );

        historyItems.addSource(historyResult, result -> {
            if (result == null) {
                return;
            }

            if (result.isLoading()) {
                isLoading = true;
                return;
            }

            isLoading = false;
            if (!result.isSuccess()) {
                return;
            }

            SponsorDonationHistoryPage pageData = result.getData();
            if (pageData == null) {
                return;
            }

            HistoryRequest request = historyTrigger.getValue();
            boolean isResetRequest = request != null && request.reset;
            if (isResetRequest) {
                cachedHistoryItems.clear();
            }

            List<SponsorDonationHistoryItem> newItems = pageData.getItems();
            if (newItems != null && !newItems.isEmpty()) {
                cachedHistoryItems.addAll(newItems);
            }

            currentPage = pageData.getPage();
            isLastPage = !pageData.isHasNext();
            historyItems.setValue(new ArrayList<>(cachedHistoryItems));
        });
    }

    public LiveData<NetworkResultWrapper<SponsorDonationHistoryPage>> getHistoryResult() {
        return historyResult;
    }

    public LiveData<List<SponsorDonationHistoryItem>> getHistoryItems() {
        return historyItems;
    }

    @Nullable
    public SponsorDonationStatus getSelectedStatus() {
        return selectedStatus;
    }

    public void loadInitialHistory(@Nullable SponsorDonationStatus status) {
        selectedStatus = status;
        currentPage = DEFAULT_PAGE;
        isLastPage = false;
        historyTrigger.setValue(new HistoryRequest(selectedStatus, DEFAULT_PAGE, PAGE_LIMIT, true));
    }

    public void loadNextPage() {
        if (isLoading || isLastPage) {
            return;
        }
        historyTrigger.setValue(new HistoryRequest(selectedStatus, currentPage + 1, PAGE_LIMIT, false));
    }

    private static final class HistoryRequest {
        @Nullable
        final SponsorDonationStatus status;
        final int page;
        final int limit;
        final boolean reset;

        HistoryRequest(@Nullable SponsorDonationStatus status, int page, int limit, boolean reset) {
            this.status = status;
            this.page = page;
            this.limit = limit;
            this.reset = reset;
        }
    }
}
