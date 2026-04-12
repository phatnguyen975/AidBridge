package com.drc.aidbridge.ui.main.viewmodel.victim;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.domain.model.victim.VictimHistoryAidItemDetail;
import com.drc.aidbridge.domain.model.victim.VictimHistoryDetail;
import com.drc.aidbridge.domain.model.victim.VictimHistoryItem;
import com.drc.aidbridge.domain.model.victim.VictimHistoryPage;
import com.drc.aidbridge.domain.usecase.victim.GetVictimHistoryDetailUseCase;
import com.drc.aidbridge.domain.usecase.victim.GetVictimHistoryUseCase;
import com.drc.aidbridge.ui.base.BaseViewModel;
import com.drc.aidbridge.ui.main.adapter.victim.VictimHistoryAdapter;

import java.io.Serializable;
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
    private final GetVictimHistoryDetailUseCase getVictimHistoryDetailUseCase;

    private final MutableLiveData<HistoryRequest> historyTrigger = new MutableLiveData<>();
    private final MutableLiveData<HistoryDetailRequest> historyDetailTrigger = new MutableLiveData<>();

    private final LiveData<NetworkResultWrapper<VictimHistoryPage>> historySource;
    private final LiveData<NetworkResultWrapper<VictimHistoryDetail>> historyDetailSource;

    private final MediatorLiveData<NetworkResultWrapper<HistoryUiPage>> historyResult =
        new MediatorLiveData<>();
    private final MediatorLiveData<NetworkResultWrapper<HistoryDetailUiModel>> historyDetailResult =
        new MediatorLiveData<>();

    private String currentTimeRange = DEFAULT_TIME_RANGE;
    private int currentPage = 1;
    private int pendingPage = 1;
    private boolean isLastPage = false;
    private boolean isLoading = false;

    @Inject
    public VictimHistoryViewModel(GetVictimHistoryUseCase getVictimHistoryUseCase,
                                  GetVictimHistoryDetailUseCase getVictimHistoryDetailUseCase) {
        this.getVictimHistoryUseCase = getVictimHistoryUseCase;
        this.getVictimHistoryDetailUseCase = getVictimHistoryDetailUseCase;

        historySource = Transformations.switchMap(historyTrigger,
            request -> this.getVictimHistoryUseCase.execute(
                request.page,
                PAGE_SIZE,
                request.timeRange,
                !request.networkAvailable
            )
        );

        historyDetailSource = Transformations.switchMap(historyDetailTrigger,
            request -> this.getVictimHistoryDetailUseCase.execute(request.requestId, request.requestType)
        );

        historyResult.addSource(historySource, this::handleSourceResult);
        historyDetailResult.addSource(historyDetailSource, this::handleDetailSourceResult);
    }

    public LiveData<NetworkResultWrapper<HistoryUiPage>> getHistoryResult() {
        return historyResult;
    }

    public LiveData<NetworkResultWrapper<HistoryDetailUiModel>> getHistoryDetailResult() {
        return historyDetailResult;
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

    public void loadDetail(VictimHistoryAdapter.HistoryModel model) {
        if (model == null) {
            historyDetailResult.postValue(NetworkResultWrapper.error("Không tìm thấy thông tin yêu cầu."));
            return;
        }

        String requestId = safeText(model.id);
        if (requestId.isEmpty()) {
            historyDetailResult.postValue(NetworkResultWrapper.error("Không tìm thấy mã yêu cầu."));
            return;
        }

        historyDetailTrigger.postValue(new HistoryDetailRequest(requestId, safeText(model.type)));
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

    private void handleDetailSourceResult(NetworkResultWrapper<VictimHistoryDetail> result) {
        if (result == null) {
            return;
        }

        if (result.isLoading()) {
            historyDetailResult.postValue(NetworkResultWrapper.loading());
            return;
        }

        if (result.isError()) {
            historyDetailResult.postValue(NetworkResultWrapper.error(result.getMessage(), 0));
            return;
        }

        VictimHistoryDetail detail = result.getData();
        if (detail == null) {
            historyDetailResult.postValue(NetworkResultWrapper.error("Không có dữ liệu chi tiết yêu cầu."));
            return;
        }

        historyDetailResult.postValue(NetworkResultWrapper.success(mapToDetailUiModel(detail)));
    }

    private List<VictimHistoryAdapter.HistoryModel> mapToUiModels(List<VictimHistoryItem> items) {
        if (items == null || items.isEmpty()) {
            return Collections.emptyList();
        }

        List<VictimHistoryAdapter.HistoryModel> models = new ArrayList<>();
        for (VictimHistoryItem item : items) {
            if (item == null) {
                continue;
            }

            String type = mapType(item.getType(), item.getTitle());
            String statusType = mapStatusType(item.getStatusType(), item.getStatus());

            models.add(new VictimHistoryAdapter.HistoryModel(
                safeText(item.getRequestId()),
                safeText(item.getTitle()),
                safeText(item.getStatus()),
                statusType,
                safeText(item.getDateTime()),
                safeText(item.getLocation()),
                type,
                safeText(item.getDetail())
            ));
        }
        return models;
    }

    private HistoryDetailUiModel mapToDetailUiModel(VictimHistoryDetail detail) {
        if (detail == null) {
            return new HistoryDetailUiModel(
                "",
                VictimHistoryAdapter.TYPE_SOS_SELF,
                "",
                VictimHistoryAdapter.HistoryModel.STATUS_PROCESSING,
                "",
                "",
                "",
                null,
                null,
                null,
                null,
                "",
                "",
                "",
                Collections.emptyList()
            );
        }

        return new HistoryDetailUiModel(
            safeText(detail.getRequestId()),
            mapType(detail.getType(), ""),
            safeText(detail.getStatus()),
            mapStatusType(detail.getStatusType(), detail.getStatus()),
            safeText(detail.getDateTime()),
            safeText(detail.getLocation()),
            safeText(detail.getCondition()),
            detail.getPeopleCount(),
            detail.getNumberAdult(),
            detail.getNumberElderly(),
            detail.getNumberChildren(),
            safeText(detail.getNoteFullName()),
            safeText(detail.getNotePhoneNumber()),
            safeText(detail.getNoteHealthDetail()),
            mapAidItems(detail.getRequestedItems())
        );
    }

    private List<HistoryDetailAidItemUiModel> mapAidItems(List<VictimHistoryAidItemDetail> items) {
        if (items == null || items.isEmpty()) {
            return Collections.emptyList();
        }

        List<HistoryDetailAidItemUiModel> uiItems = new ArrayList<>();
        for (VictimHistoryAidItemDetail item : items) {
            if (item == null) {
                continue;
            }
            uiItems.add(new HistoryDetailAidItemUiModel(
                safeText(item.getCategoryName()),
                item.getQuantity(),
                safeText(item.getUnit())
            ));
        }
        return uiItems;
    }

    private String mapType(String rawType, String title) {
        String normalizedType = safeText(rawType).toLowerCase(Locale.US);
        if ("supply".equals(normalizedType)
            || normalizedType.contains("supply")
            || normalizedType.contains("relief")) {
            return VictimHistoryAdapter.TYPE_SUPPLY;
        }

        if ("relative".equals(normalizedType)
            || normalizedType.contains("relative")
            || normalizedType.contains("nguoi than")) {
            return VictimHistoryAdapter.TYPE_SOS_RELATIVE;
        }

        if ("self".equals(normalizedType)
            || normalizedType.contains("self")
            || normalizedType.contains("ban than")) {
            return VictimHistoryAdapter.TYPE_SOS_SELF;
        }

        String merged = (normalizedType + " " + safeText(title)).toLowerCase(Locale.US);
        if (merged.contains("relative") || merged.contains("nguoi than")) {
            return VictimHistoryAdapter.TYPE_SOS_RELATIVE;
        }

        if (merged.contains("supply") || merged.contains("relief") || merged.contains("food")) {
            return VictimHistoryAdapter.TYPE_SUPPLY;
        }

        return VictimHistoryAdapter.TYPE_SOS_SELF;
    }

    private String mapStatusType(String rawStatusType, String statusLabel) {
        String merged = (safeText(rawStatusType) + " " + safeText(statusLabel)).toLowerCase(Locale.US);
        if (merged.contains("pending") || merged.contains("wait") || merged.contains("cho")) {
            return VictimHistoryAdapter.HistoryModel.STATUS_PENDING;
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

    private static final class HistoryDetailRequest {
        final String requestId;
        final String requestType;

        HistoryDetailRequest(String requestId, String requestType) {
            this.requestId = requestId;
            this.requestType = requestType;
        }
    }

    public static final class HistoryDetailUiModel implements Serializable {
        public final String id;
        public final String type;
        public final String status;
        public final String statusType;
        public final String date;
        public final String location;
        public final String condition;
        public final Integer peopleCount;
        public final Integer numberAdult;
        public final Integer numberElderly;
        public final Integer numberChildren;
        public final String noteFullName;
        public final String notePhoneNumber;
        public final String noteHealthDetail;
        public final List<HistoryDetailAidItemUiModel> requestedItems;

        public HistoryDetailUiModel(String id,
                                    String type,
                                    String status,
                                    String statusType,
                                    String date,
                                    String location,
                                    String condition,
                                    Integer peopleCount,
                                    Integer numberAdult,
                                    Integer numberElderly,
                                    Integer numberChildren,
                                    String noteFullName,
                                    String notePhoneNumber,
                                    String noteHealthDetail,
                                    List<HistoryDetailAidItemUiModel> requestedItems) {
            this.id = id;
            this.type = type;
            this.status = status;
            this.statusType = statusType;
            this.date = date;
            this.location = location;
            this.condition = condition;
            this.peopleCount = peopleCount;
            this.numberAdult = numberAdult;
            this.numberElderly = numberElderly;
            this.numberChildren = numberChildren;
            this.noteFullName = noteFullName;
            this.notePhoneNumber = notePhoneNumber;
            this.noteHealthDetail = noteHealthDetail;
            this.requestedItems = requestedItems != null ? requestedItems : Collections.emptyList();
        }
    }

    public static final class HistoryDetailAidItemUiModel implements Serializable {
        public final String categoryName;
        public final int quantity;
        public final String unit;

        public HistoryDetailAidItemUiModel(String categoryName, int quantity, String unit) {
            this.categoryName = categoryName;
            this.quantity = quantity;
            this.unit = unit;
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
