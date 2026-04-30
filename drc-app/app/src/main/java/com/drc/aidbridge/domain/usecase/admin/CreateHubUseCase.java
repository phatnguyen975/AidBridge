package com.drc.aidbridge.domain.usecase.admin;

import androidx.lifecycle.LiveData;

import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.domain.model.admin.Hub;
import com.drc.aidbridge.domain.repository.admin.HubRepository;

import javax.inject.Inject;

public class CreateHubUseCase {

    private final HubRepository hubRepository;

    @Inject
    public CreateHubUseCase(HubRepository hubRepository) {
        this.hubRepository = hubRepository;
    }

    public LiveData<NetworkResultWrapper<Hub>> execute(String name,
                                                       String address,
                                                       String phoneNumber,
                                                       String imageUrl,
                                                       String operatingHours,
                                                       Double latitude,
                                                       Double longitude) {
        return hubRepository.createHub(
                name,
                address,
                phoneNumber,
                imageUrl,
                operatingHours,
                latitude,
                longitude);
    }
}
