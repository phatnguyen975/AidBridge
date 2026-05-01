package com.drc.aidbridge.ui.main.viewmodel.admin;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.data.remote.dto.response.admin.AdminRoutingSosAidResponseDto;
import com.drc.aidbridge.domain.repository.HubRepository;
import com.drc.aidbridge.domain.repository.admin.AdminDashboardRepository;
import com.drc.aidbridge.domain.usecase.routing.CalculateRouteUseCase;
import com.drc.aidbridge.ui.map.base.BaseMapViewModel;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class AdminMapViewModel extends BaseMapViewModel {

    private final AdminDashboardRepository adminDashboardRepository;
    private final MutableLiveData<NetworkResultWrapper<AdminRoutingSosAidResponseDto>> sosAidRequests = new MutableLiveData<>();

    @Inject
    public AdminMapViewModel(CalculateRouteUseCase calculateRouteUseCase,
                             HubRepository hubRepository,
                             AdminDashboardRepository adminDashboardRepository) {
        super(calculateRouteUseCase, hubRepository);
        this.adminDashboardRepository = adminDashboardRepository;
    }

    public LiveData<NetworkResultWrapper<AdminRoutingSosAidResponseDto>> getSosAidRequests() {
        return sosAidRequests;
    }

    public void fetchSosAidRequests(String status, String startDate, String endDate) {
        adminDashboardRepository.getSosAidRequests(status, startDate, endDate).observeForever(result -> {
            if (result != null) {
                sosAidRequests.postValue(result);
            }
        });
    }
}
