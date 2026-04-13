package com.drc.aidbridge.ui.main.viewmodel.volunteer;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.domain.model.volunteer.VolunteerHistoryItem;
import com.drc.aidbridge.domain.usecase.volunteer.GetVolunteerMissionHistoryUseCase;
import com.drc.aidbridge.ui.base.BaseViewModel;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class VolunteerHistoryViewModel extends BaseViewModel {

    private static final String FILTER_ALL = "all";

    private final MutableLiveData<Boolean> loadTrigger = new MutableLiveData<>();
    private final MutableLiveData<String> selectedFilter = new MutableLiveData<>(FILTER_ALL);
    private final LiveData<NetworkResultWrapper<List<VolunteerHistoryItem>>> historyResult;
    private final MediatorLiveData<List<VolunteerHistoryItem>> filteredHistoryList = new MediatorLiveData<>();

    private List<VolunteerHistoryItem> cachedHistoryList = new ArrayList<>();

    @Inject
    public VolunteerHistoryViewModel(GetVolunteerMissionHistoryUseCase getVolunteerMissionHistoryUseCase) {
        this.historyResult = Transformations.switchMap(
                loadTrigger,
                trigger -> getVolunteerMissionHistoryUseCase.execute());

        initFilteredStreams();
    }

    private void initFilteredStreams() {
        filteredHistoryList.addSource(historyResult, result -> {
            if (result != null && result.isSuccess()) {
                List<VolunteerHistoryItem> data = result.getData();
                cachedHistoryList = data != null ? new ArrayList<>(data) : new ArrayList<>();
                applyFilter(selectedFilter.getValue());
            }
        });

        filteredHistoryList.addSource(selectedFilter, this::applyFilter);
    }

    public LiveData<NetworkResultWrapper<List<VolunteerHistoryItem>>> getHistoryResult() {
        return historyResult;
    }

    public LiveData<List<VolunteerHistoryItem>> getFilteredHistoryList() {
        return filteredHistoryList;
    }

    public void loadHistory() {
        loadTrigger.setValue(Boolean.TRUE);
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
}
