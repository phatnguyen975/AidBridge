package com.drc.aidbridge.ui.main.viewmodel.victim;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.data.remote.dto.response.victim.VictimHistoryDto;
import com.drc.aidbridge.domain.model.victim.VictimHistoryPage;
import com.drc.aidbridge.domain.usecase.victim.GetVictimHistoryUseCase;
import com.drc.aidbridge.ui.base.BaseViewModel;
import com.drc.aidbridge.ui.main.adapter.victim.VictimHistoryAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class VictimHistoryViewModel extends BaseViewModel {

    private static final int PAGE_SIZE = 10;
    private static final String DEFAULT_TIME_RANGE = "1h";

    private final ExecutorService workerExecutor = Executors.newSingleThreadExecutor();
    private final GetVictimHistoryUseCase getVictimHistoryUseCase;

    private final MutableLiveData<HistoryRequest> historyTrigger = new MutableLiveData<>();
    private final LiveData<NetworkResultWrapper<VictimHistoryPage>> historySource;

    private final MediatorLiveData<NetworkResultWrapper<HistoryUiPage>> historyResult =
        new MediatorLiveData<>();

    private String currentTimeRange = DEFAULT_TIME_RANGE;
    private int currentPage = 1;
    private int pendingPage = 1;
    private boolean isLastPage = false;
    private boolean isLoading = false;

    @Inject
    public VictimHistoryViewModel(GetVictimHistoryUseCase getVictimHistoryUseCase) {
        this.getVictimHistoryUseCase = getVictimHistoryUseCase;

        historySource = Transformations.switchMap(historyTrigger,
            request -> this.getVictimHistoryUseCase.execute(
                request.page,
                PAGE_SIZE,
                request.timeRange,
                !request.networkAvailable
            )
        );

        historyResult.addSource(historySource, this::handleSourceResult);
    }

    public LiveData<NetworkResultWrapper<HistoryUiPage>> getHistoryResult() {
        return historyResult;
    }

    public void loadInitial(boolean networkAvailable) {
        currentPage = 1;
        isLastPage = false;
        triggerLoad(1, networkAvailable);
    }

    public void refresh(boolean networkAvailable) {
        currentPage = 1;
        isLastPage = false;
        triggerLoad(1, networkAvailable);
    }

    public void applyTimeRange(String timeRange, boolean networkAvailable) {
        currentTimeRange = normalizeTimeRange(timeRange);
        currentPage = 1;
        isLastPage = false;
        triggerLoad(1, networkAvailable);
    }

    public void loadNextPage(boolean networkAvailable) {
        if (isLoading || isLastPage) {
            return;
        }
        triggerLoad(currentPage + 1, networkAvailable);
    }

    @Override
    protected void onCleared() {
        workerExecutor.shutdownNow();
        super.onCleared();
    }

    private void triggerLoad(int page, boolean networkAvailable) {
        if (isLoading) {
            return;
        }

        pendingPage = Math.max(1, page);
        isLoading = true;

        workerExecutor.execute(() -> historyTrigger.postValue(
            new HistoryRequest(pendingPage, currentTimeRange, networkAvailable)
        ));
    }

    private void handleSourceResult(NetworkResultWrapper<VictimHistoryPage> result) {
        if (result == null) {
            return;
        }

        if (result.isLoading()) {
            historyResult.postValue(NetworkResultWrapper.loading());
            return;
        }

        if (result.isError()) {
            isLoading = false;
            historyResult.postValue(NetworkResultWrapper.error(result.getMessage(), 0));
            return;
        }

        VictimHistoryPage pageData = result.getData();
        List<VictimHistoryAdapter.HistoryModel> mappedItems = mapToUiModels(
            pageData != null ? pageData.getItems() : Collections.emptyList()
        );

        currentPage = pendingPage;
        isLastPage = pageData == null || !pageData.hasNextPage();
        isLoading = false;

        historyResult.postValue(NetworkResultWrapper.success(new HistoryUiPage(
            mappedItems,
            currentPage > 1,
            isLastPage,
            pageData != null && pageData.isOfflineData()
        )));
    }

    private List<VictimHistoryAdapter.HistoryModel> mapToUiModels(List<VictimHistoryDto> dtos) {
        if (dtos == null || dtos.isEmpty()) {
            return Collections.emptyList();
        }

        List<VictimHistoryAdapter.HistoryModel> models = new ArrayList<>();
        for (VictimHistoryDto dto : dtos) {
            if (dto == null) {
                continue;
            }

            String type = mapType(dto.getType(), dto.getTitle());
            String statusType = mapStatusType(dto.getStatusType(), dto.getStatus());

            models.add(new VictimHistoryAdapter.HistoryModel(
                safeText(dto.getRequestId()),
                safeText(dto.getTitle()),
                safeText(dto.getStatus()),
                statusType,
                safeText(dto.getDateTime()),
                safeText(dto.getLocation()),
                type,
                safeText(dto.getDetail())
            ));
        }
        return models;
    }

    private String mapType(String rawType, String title) {
        String merged = (safeText(rawType) + " " + safeText(title)).toLowerCase(Locale.US);
        if (merged.contains("supply") || merged.contains("relief") || merged.contains("food")) {
            return VictimHistoryAdapter.TYPE_SUPPLY;
        }
        if (merged.contains("relative")) {
            return VictimHistoryAdapter.TYPE_SOS_RELATIVE;
        }
        return VictimHistoryAdapter.TYPE_SOS_SELF;
    }

    private String mapStatusType(String rawStatusType, String statusLabel) {
        String merged = (safeText(rawStatusType) + " " + safeText(statusLabel)).toLowerCase(Locale.US);
        if (merged.contains("pending") || merged.contains("wait")) {
            return VictimHistoryAdapter.HistoryModel.STATUS_PENDING;
        }
        if (merged.contains("process") || merged.contains("assign") || merged.contains("handle")) {
            return VictimHistoryAdapter.HistoryModel.STATUS_PROCESSING;
        }
        if (merged.contains("done") || merged.contains("complete") || merged.contains("finish")) {
            return VictimHistoryAdapter.HistoryModel.STATUS_COMPLETED;
        }
        return VictimHistoryAdapter.HistoryModel.STATUS_PROCESSING;
    }

    private String normalizeTimeRange(String timeRange) {
        String value = safeText(timeRange).toLowerCase(Locale.US);
        switch (value) {
            case "24h":
            case "7d":
            case "1m":
            case "all":
            case "1h":
                return value;
            default:
                return DEFAULT_TIME_RANGE;
        }
    }

    private String safeText(String value) {
        return value != null ? value.trim() : "";
    }

    private static final class HistoryRequest {
        final int page;
        final String timeRange;
        final boolean networkAvailable;

        HistoryRequest(int page, String timeRange, boolean networkAvailable) {
            this.page = page;
            this.timeRange = timeRange;
            this.networkAvailable = networkAvailable;
        }
    }

    public static final class HistoryUiPage {
        private final List<VictimHistoryAdapter.HistoryModel> items;
        private final boolean append;
        private final boolean lastPage;
        private final boolean offlineData;

        public HistoryUiPage(List<VictimHistoryAdapter.HistoryModel> items,
                             boolean append,
                             boolean lastPage,
                             boolean offlineData) {
            this.items = items != null ? items : Collections.emptyList();
            this.append = append;
            this.lastPage = lastPage;
            this.offlineData = offlineData;
        }

        public List<VictimHistoryAdapter.HistoryModel> getItems() {
            return items;
        }

        public boolean isAppend() {
            return append;
        }

        public boolean isLastPage() {
            return lastPage;
        }

        public boolean isOfflineData() {
            return offlineData;
        }
    }
}
