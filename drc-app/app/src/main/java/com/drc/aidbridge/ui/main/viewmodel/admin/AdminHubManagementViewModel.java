package com.drc.aidbridge.ui.main.viewmodel.admin;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.domain.enums.HubStatus;
import com.drc.aidbridge.domain.model.admin.Hub;
import com.drc.aidbridge.domain.usecase.admin.ListHubsUseCase;
import com.drc.aidbridge.domain.usecase.admin.UpdateHubStatusUseCase;
import com.drc.aidbridge.ui.base.BaseViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class AdminHubManagementViewModel extends BaseViewModel {

    private final MutableLiveData<Long> fetchHubsTrigger = new MutableLiveData<>();
    private final MutableLiveData<ToggleHubStatusParams> toggleHubStatusTrigger = new MutableLiveData<>();
    private final MutableLiveData<String> searchQuery = new MutableLiveData<>("");
    private final MutableLiveData<List<Hub>> sourceHubs = new MutableLiveData<>(new ArrayList<>());

    private final LiveData<NetworkResultWrapper<List<Hub>>> fetchHubsResult;
    private final LiveData<NetworkResultWrapper<Hub>> toggleHubStatusResult;
    private final MediatorLiveData<NetworkResultWrapper<List<Hub>>> hubsResult = new MediatorLiveData<>();

    private final LiveData<Integer> totalHubsCount;
    private final LiveData<Integer> activeHubsCount;

    private NetworkResultWrapper<List<Hub>> latestFetchResult;

    @Inject
    public AdminHubManagementViewModel(ListHubsUseCase listHubsUseCase,
            UpdateHubStatusUseCase updateHubStatusUseCase) {
        this.fetchHubsResult = Transformations.switchMap(
                fetchHubsTrigger,
                ignored -> listHubsUseCase.execute());

        this.toggleHubStatusResult = Transformations.switchMap(
                toggleHubStatusTrigger,
                params -> updateHubStatusUseCase.execute(params.hubId, params.nextStatus));

        this.totalHubsCount = Transformations.map(sourceHubs, hubs -> hubs != null ? hubs.size() : 0);
        this.activeHubsCount = Transformations.map(sourceHubs, this::countActiveHubs);

        hubsResult.addSource(fetchHubsResult, this::handleFetchHubsResult);
        hubsResult.addSource(searchQuery, ignored -> publishFilteredHubList());

        hubsResult.addSource(toggleHubStatusResult, result -> {
            if (result != null && result.isSuccess()) {
                fetchHubs();
            }
        });
    }

    public LiveData<NetworkResultWrapper<List<Hub>>> getHubsResult() {
        return hubsResult;
    }

    public LiveData<NetworkResultWrapper<Hub>> getToggleHubStatusResult() {
        return toggleHubStatusResult;
    }

    public LiveData<Integer> getTotalHubsCount() {
        return totalHubsCount;
    }

    public LiveData<Integer> getActiveHubsCount() {
        return activeHubsCount;
    }

    public void fetchHubs() {
        fetchHubsTrigger.setValue(System.currentTimeMillis());
    }

    public void updateSearchQuery(String query) {
        searchQuery.setValue(query != null ? query : "");
    }

    public void toggleHubStatus(UUID hubId) {
        Hub currentHub = findHubById(hubId);
        if (currentHub == null) {
            return;
        }

        HubStatus nextStatus = currentHub.getStatus() == HubStatus.ACTIVE
                ? HubStatus.INACTIVE
                : HubStatus.ACTIVE;
        toggleHubStatusTrigger.setValue(new ToggleHubStatusParams(hubId, nextStatus));
    }

    private void handleFetchHubsResult(NetworkResultWrapper<List<Hub>> result) {
        if (result == null) {
            hubsResult.setValue(NetworkResultWrapper.error("Dữ liệu danh sách trạm không hợp lệ."));
            return;
        }

        latestFetchResult = result;
        if (result.isLoading()) {
            hubsResult.setValue(NetworkResultWrapper.loading());
            return;
        }

        if (result.isError()) {
            hubsResult.setValue(NetworkResultWrapper.error(result.getMessage()));
            return;
        }

        List<Hub> fetchedHubs = result.getData() != null ? result.getData() : new ArrayList<>();
        sourceHubs.setValue(new ArrayList<>(fetchedHubs));
        publishFilteredHubList();
    }

    private void publishFilteredHubList() {
        if (latestFetchResult == null || !latestFetchResult.isSuccess()) {
            return;
        }

        List<Hub> hubs = sourceHubs.getValue();
        List<Hub> filtered = filterHubs(hubs, searchQuery.getValue());
        hubsResult.setValue(NetworkResultWrapper.success(filtered));
    }

    private Hub findHubById(UUID hubId) {
        if (hubId == null) {
            return null;
        }

        List<Hub> hubs = sourceHubs.getValue();
        if (hubs == null) {
            return null;
        }

        for (Hub hub : hubs) {
            if (hub != null && hubId.equals(hub.getId())) {
                return hub;
            }
        }
        return null;
    }

    private List<Hub> filterHubs(List<Hub> hubs, String query) {
        List<Hub> source = hubs != null ? hubs : new ArrayList<>();
        String normalizedQuery = query != null ? query.trim().toLowerCase(Locale.getDefault()) : "";
        if (normalizedQuery.isEmpty()) {
            return new ArrayList<>(source);
        }

        List<Hub> filtered = new ArrayList<>();
        for (Hub hub : source) {
            if (hub == null) {
                continue;
            }

            String hubName = safeText(hub.getName()).toLowerCase(Locale.getDefault());
            String hubAddress = safeText(hub.getAddress()).toLowerCase(Locale.getDefault());
            if (hubName.contains(normalizedQuery) || hubAddress.contains(normalizedQuery)) {
                filtered.add(hub);
            }
        }
        return filtered;
    }

    private int countActiveHubs(List<Hub> hubs) {
        if (hubs == null || hubs.isEmpty()) {
            return 0;
        }

        int activeCount = 0;
        for (Hub hub : hubs) {
            if (hub != null && hub.getStatus() == HubStatus.ACTIVE) {
                activeCount++;
            }
        }
        return activeCount;
    }

    private String safeText(String value) {
        return value == null ? "" : value;
    }

    private static final class ToggleHubStatusParams {
        final UUID hubId;
        final HubStatus nextStatus;

        ToggleHubStatusParams(UUID hubId, HubStatus nextStatus) {
            this.hubId = hubId;
            this.nextStatus = nextStatus;
        }
    }
}