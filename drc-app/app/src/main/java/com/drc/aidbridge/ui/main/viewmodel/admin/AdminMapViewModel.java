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

import com.drc.aidbridge.data.remote.dto.request.DangerousZoneRequestDto;
import com.drc.aidbridge.data.remote.dto.response.DangerousZoneResponseDto;
import com.drc.aidbridge.domain.repository.RoutingRepository;
import com.drc.aidbridge.domain.repository.UserRepository;

import java.util.List;
import java.util.UUID;

@HiltViewModel
public class AdminMapViewModel extends BaseMapViewModel {

    private final AdminDashboardRepository adminDashboardRepository;
    private final RoutingRepository routingRepository;
    private final UserRepository userRepository;
    
    private UUID currentAdminId = null;

    private final MutableLiveData<NetworkResultWrapper<AdminRoutingSosAidResponseDto>> sosAidRequests = new MutableLiveData<>();
    private final MutableLiveData<NetworkResultWrapper<DangerousZoneResponseDto>> zoneOperationResult = new MutableLiveData<>();
    private final MutableLiveData<NetworkResultWrapper<Void>> zoneDeleteResult = new MutableLiveData<>();

    @Inject
    public AdminMapViewModel(CalculateRouteUseCase calculateRouteUseCase,
                             HubRepository hubRepository,
                             AdminDashboardRepository adminDashboardRepository,
                             RoutingRepository routingRepository,
                             UserRepository userRepository) {
        super(calculateRouteUseCase, hubRepository, routingRepository);
        this.adminDashboardRepository = adminDashboardRepository;
        this.routingRepository = routingRepository;
        this.userRepository = userRepository;
        
        loadCurrentAdminId();
    }

    private void loadCurrentAdminId() {
        userRepository.getCachedUser().observeForever(result -> {
            if (result != null && result.isSuccess() && result.getData() != null) {
                try {
                    currentAdminId = UUID.fromString(result.getData().getId());
                } catch (Exception ignored) {}
            }
        });
    }

    public UUID getCurrentAdminId() {
        return currentAdminId;
    }

    public LiveData<NetworkResultWrapper<AdminRoutingSosAidResponseDto>> getSosAidRequests() {
        return sosAidRequests;
    }
    
    public LiveData<NetworkResultWrapper<DangerousZoneResponseDto>> getZoneOperationResult() {
        return zoneOperationResult;
    }
    
    public LiveData<NetworkResultWrapper<Void>> getZoneDeleteResult() {
        return zoneDeleteResult;
    }

    public void fetchSosAidRequests(String status, String startDate, String endDate) {
        adminDashboardRepository.getSosAidRequests(status, startDate, endDate).observeForever(result -> {
            if (result != null) {
                sosAidRequests.postValue(result);
            }
        });
    }

    public void saveDangerousZone(UUID id, DangerousZoneRequestDto request) {
        LiveData<NetworkResultWrapper<DangerousZoneResponseDto>> call;
        if (id == null) {
            call = routingRepository.createDangerousZone(request);
        } else {
            call = routingRepository.updateDangerousZone(id, request);
        }
        
        call.observeForever(result -> {
            if (result != null) {
                zoneOperationResult.postValue(result);
                if (result.isSuccess()) {
                    fetchDangerousZones(); // Refresh list after save
                }
            }
        });
    }
    
    public void deleteDangerousZone(UUID id) {
        routingRepository.deleteDangerousZone(id).observeForever(result -> {
            if (result != null) {
                zoneDeleteResult.postValue(result);
                if (result.isSuccess()) {
                    fetchDangerousZones(); // Refresh list after delete
                }
            }
        });
    }
}
