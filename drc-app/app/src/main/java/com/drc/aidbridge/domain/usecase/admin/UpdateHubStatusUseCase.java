package com.drc.aidbridge.domain.usecase.admin;

import androidx.lifecycle.LiveData;

import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.domain.enums.HubStatus;
import com.drc.aidbridge.domain.model.admin.Hub;
import com.drc.aidbridge.domain.repository.admin.HubRepository;

import java.util.UUID;

import javax.inject.Inject;

public class UpdateHubStatusUseCase {

    private final HubRepository hubRepository;

    @Inject
    public UpdateHubStatusUseCase(HubRepository hubRepository) {
        this.hubRepository = hubRepository;
    }

    public LiveData<NetworkResultWrapper<Hub>> execute(UUID hubId, HubStatus status) {
        return hubRepository.updateHubStatus(hubId, status);
    }
}
