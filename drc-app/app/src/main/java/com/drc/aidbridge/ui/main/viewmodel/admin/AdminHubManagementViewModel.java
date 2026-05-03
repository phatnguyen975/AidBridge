package com.drc.aidbridge.ui.main.viewmodel.admin;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.domain.enums.HubStatus;
import com.drc.aidbridge.domain.model.admin.Hub;
import com.drc.aidbridge.domain.model.admin.HubSummary;
import com.drc.aidbridge.domain.usecase.admin.ListHubsUseCase;
import com.drc.aidbridge.domain.usecase.admin.UpdateHubStatusUseCase;
import com.drc.aidbridge.ui.base.BaseViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class AdminHubManagementViewModel extends BaseViewModel {

    private final MutableLiveData<Long> fetchHubsTrigger = new MutableLiveData<>();
    private final MutableLiveData<ToggleHubStatusParams> toggleHubStatusTrigger = new MutableLiveData<>();
    private final MutableLiveData<String> searchQuery = new MutableLiveData<>("");
    private final MutableLiveData<List<Hub>> sourceHubs = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<HubSummary> hubSummary = new MutableLiveData<>(new HubSummary(0, 0, 0));

    private final LiveData<NetworkResultWrapper<List<Hub>>> fetchHubsResult;
    private final LiveData<NetworkResultWrapper<Hub>> toggleHubStatusResult;
    private final MediatorLiveData<NetworkResultWrapper<List<Hub>>> hubsResult = new MediatorLiveData<>();

    @Inject
    public AdminHubManagementViewModel(ListHubsUseCase listHubsUseCase,
            UpdateHubStatusUseCase updateHubStatusUseCase) {
        this.fetchHubsResult = Transformations.switchMap(
                fetchHubsTrigger,
                ignored -> listHubsUseCase.execute());

        this.toggleHubStatusResult = Transformations.switchMap(
                toggleHubStatusTrigger,
                params -> updateHubStatusUseCase.execute(params.hubId, params.nextStatus));

        hubsResult.addSource(fetchHubsResult, this::handleFetchHubsResult);
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

    public LiveData<HubSummary> getHubSummary() {
        return hubSummary;
    }

    public void fetchHubs() {
        fetchHubsTrigger.setValue(System.currentTimeMillis());
    }

    public void updateSearchQuery(String query) {
        String safeQuery = query != null ? query : "";
        searchQuery.setValue(safeQuery);
        hubsResult.setValue(NetworkResultWrapper.success(filterHubs(sourceHubs.getValue(), safeQuery)));
    }

    public void toggleHubStatus(UUID hubId) {
        Hub currentHub = findHubById(hubId);
        if (currentHub == null) {
            return;
        }

        HubStatus currentStatus = HubStatus.fromStringSafe(currentHub.getStatus());
        HubStatus nextStatus = currentStatus == HubStatus.ACTIVE
                ? HubStatus.INACTIVE
                : HubStatus.ACTIVE;
        toggleHubStatusTrigger.setValue(new ToggleHubStatusParams(hubId, nextStatus));
    }

    private void handleFetchHubsResult(NetworkResultWrapper<List<Hub>> result) {
        if (result == null) {
            hubsResult.setValue(NetworkResultWrapper.error("Du lieu danh sach tram khong hop le."));
            return;
        }

        if (result.isLoading()) {
            hubsResult.setValue(NetworkResultWrapper.loading());
            return;
        }

        if (result.isError()) {
            hubSummary.setValue(new HubSummary(0, 0, 0));
            hubsResult.setValue(NetworkResultWrapper.error(result.getMessage()));
            return;
        }

        List<Hub> fetchedHubs = result.getData() != null ? result.getData() : new ArrayList<>();
        sourceHubs.setValue(new ArrayList<>(fetchedHubs));
        hubSummary.setValue(buildSummary(fetchedHubs));
        hubsResult.setValue(NetworkResultWrapper.success(filterHubs(fetchedHubs, searchQuery.getValue())));
    }

    private HubSummary buildSummary(List<Hub> hubs) {
        if (hubs == null || hubs.isEmpty()) {
            return new HubSummary(0, 0, 0);
        }

        int active = 0;
        int inactive = 0;
        for (Hub hub : hubs) {
            HubStatus status = HubStatus.fromStringSafe(hub != null ? hub.getStatus() : null);
            if (status == HubStatus.ACTIVE) {
                active++;
            } else if (status == HubStatus.INACTIVE) {
                inactive++;
            }
        }
        return new HubSummary(hubs.size(), active, inactive);
    }

    private List<Hub> filterHubs(List<Hub> hubs, String rawQuery) {
        List<Hub> source = hubs != null ? hubs : new ArrayList<>();
        String query = rawQuery != null ? rawQuery.trim().toLowerCase() : "";
        if (query.isEmpty()) {
            return new ArrayList<>(source);
        }

        List<Hub> filtered = new ArrayList<>();
        for (Hub hub : source) {
            if (hub == null) {
                continue;
            }

            String name = hub.getName() != null ? hub.getName().toLowerCase() : "";
            String address = hub.getAddress() != null ? hub.getAddress().toLowerCase() : "";
            if (name.contains(query) || address.contains(query)) {
                filtered.add(hub);
            }
        }
        return filtered;
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

    private static final class ToggleHubStatusParams {
        final UUID hubId;
        final HubStatus nextStatus;

        ToggleHubStatusParams(UUID hubId, HubStatus nextStatus) {
            this.hubId = hubId;
            this.nextStatus = nextStatus;
        }
    }
}
