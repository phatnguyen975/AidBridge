package com.drc.aidbridge.ui.main.viewmodel.volunteer;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.domain.model.volunteer.VolunteerHistoryItem;
import com.drc.aidbridge.domain.model.volunteer.VolunteerHistoryPage;
import com.drc.aidbridge.domain.usecase.volunteer.GetVolunteerMissionHistoryUseCase;
import com.drc.aidbridge.ui.base.BaseViewModel;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class VolunteerHistoryViewModel extends BaseViewModel {

    private static final String FILTER_ALL = "all";
    private static final int DEFAULT_PAGE = 1;
    private static final int PAGE_LIMIT = 10;

    private final MutableLiveData<HistoryRequest> historyTrigger = new MutableLiveData<>();
    private final MutableLiveData<String> selectedFilter = new MutableLiveData<>(FILTER_ALL);
    private final LiveData<NetworkResultWrapper<VolunteerHistoryPage>> historyResult;
    private final MediatorLiveData<List<VolunteerHistoryItem>> filteredHistoryList = new MediatorLiveData<>();

    private final List<VolunteerHistoryItem> cachedHistoryList = new ArrayList<>();
    private int currentPage = DEFAULT_PAGE;
    private boolean isLoading = false;
    private boolean isLastPage = false;

    @Inject
    public VolunteerHistoryViewModel(GetVolunteerMissionHistoryUseCase getVolunteerMissionHistoryUseCase) {
        this.historyResult = Transformations.switchMap(
                historyTrigger,
                request -> getVolunteerMissionHistoryUseCase.execute(request.page, request.limit));

        initFilteredStreams();
    }

    private void initFilteredStreams() {
        filteredHistoryList.addSource(historyResult, result -> {
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

            VolunteerHistoryPage pageData = result.getData();
            if (pageData == null) {
                return;
            }

            HistoryRequest request = historyTrigger.getValue();
            boolean isResetRequest = request != null && request.reset;
            if (isResetRequest) {
                cachedHistoryList.clear();
            }

            List<VolunteerHistoryItem> items = pageData.getItems();
            if (items != null && !items.isEmpty()) {
                cachedHistoryList.addAll(items);
            }

            currentPage = pageData.getPage();
            isLastPage = !pageData.isHasNext();
            applyFilter(selectedFilter.getValue());
        });

        filteredHistoryList.addSource(selectedFilter, this::applyFilter);
    }

    public LiveData<NetworkResultWrapper<VolunteerHistoryPage>> getHistoryResult() {
        return historyResult;
    }

    public LiveData<List<VolunteerHistoryItem>> getFilteredHistoryList() {
        return filteredHistoryList;
    }

    public void loadHistory() {
        currentPage = DEFAULT_PAGE;
        isLastPage = false;
        historyTrigger.setValue(new HistoryRequest(DEFAULT_PAGE, PAGE_LIMIT, true));
    }

    public void loadNextPage() {
        if (isLoading || isLastPage) {
            return;
        }
        historyTrigger.setValue(new HistoryRequest(currentPage + 1, PAGE_LIMIT, false));
    }

    public void filterHistory(String type) {
        selectedFilter.setValue(type != null ? type : FILTER_ALL);
    }

    private void applyFilter(String filter) {
        if (filter == null || filter.trim().isEmpty() || FILTER_ALL.equalsIgnoreCase(filter.trim())) {
            filteredHistoryList.setValue(new ArrayList<>(cachedHistoryList));
            return;
        }

        List<VolunteerHistoryItem> filtered = new ArrayList<>();
        for (VolunteerHistoryItem item : cachedHistoryList) {
            if (item.getType() != null && item.getType().equalsIgnoreCase(filter.trim())) {
                filtered.add(item);
            }
        }
        filteredHistoryList.setValue(filtered);
    }

    private static final class HistoryRequest {
        final int page;
        final int limit;
        final boolean reset;

        HistoryRequest(int page, int limit, boolean reset) {
            this.page = page;
            this.limit = limit;
            this.reset = reset;
        }
    }
}
