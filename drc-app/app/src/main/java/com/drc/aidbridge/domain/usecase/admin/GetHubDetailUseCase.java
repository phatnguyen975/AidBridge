package com.drc.aidbridge.domain.usecase.admin;

import androidx.lifecycle.LiveData;

import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.domain.model.admin.Hub;
import com.drc.aidbridge.domain.repository.admin.HubRepository;

import java.util.UUID;

import javax.inject.Inject;

public class GetHubDetailUseCase {

    private final HubRepository hubRepository;

    @Inject
    public GetHubDetailUseCase(HubRepository hubRepository) {
        this.hubRepository = hubRepository;
    }

    public LiveData<NetworkResultWrapper<Hub>> execute(UUID hubId) {
        return hubRepository.getHubDetail(hubId);
    }
}
