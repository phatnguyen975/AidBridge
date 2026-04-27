package com.drc.aidbridge.domain.usecase.admin;

import androidx.lifecycle.LiveData;

import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.domain.model.admin.Hub;
import com.drc.aidbridge.domain.repository.admin.HubRepository;

import java.util.List;

import javax.inject.Inject;

public class ListHubsUseCase {

    private final HubRepository hubRepository;

    @Inject
    public ListHubsUseCase(HubRepository hubRepository) {
        this.hubRepository = hubRepository;
    }

    public LiveData<NetworkResultWrapper<List<Hub>>> execute() {
        return hubRepository.listHubs();
    }
}
