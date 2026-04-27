package com.drc.aidbridge.ui.main.viewmodel.admin;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.domain.model.admin.Hub;
import com.drc.aidbridge.domain.usecase.admin.GetHubDetailUseCase;
import com.drc.aidbridge.ui.base.BaseViewModel;

import java.util.UUID;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class AdminHubDetailViewModel extends BaseViewModel {

    private final MutableLiveData<UUID> fetchHubDetailTrigger = new MutableLiveData<>();
    private final LiveData<NetworkResultWrapper<Hub>> fetchHubDetailResult;
    private final MediatorLiveData<NetworkResultWrapper<Hub>> hubDetailResult = new MediatorLiveData<>();

    @Inject
    public AdminHubDetailViewModel(GetHubDetailUseCase getHubDetailUseCase) {
        this.fetchHubDetailResult = Transformations.switchMap(
                fetchHubDetailTrigger,
                getHubDetailUseCase::execute);

        hubDetailResult.addSource(fetchHubDetailResult, hubDetailResult::setValue);
    }

    public LiveData<NetworkResultWrapper<Hub>> getHubDetailResult() {
        return hubDetailResult;
    }

    public void fetchHubDetail(UUID hubId) {
        if (hubId == null) {
            return;
        }

        fetchHubDetailTrigger.setValue(hubId);
    }
}
