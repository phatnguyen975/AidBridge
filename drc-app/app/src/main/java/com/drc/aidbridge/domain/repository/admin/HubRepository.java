package com.drc.aidbridge.domain.repository.admin;

import androidx.lifecycle.LiveData;

import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.domain.enums.HubStatus;
import com.drc.aidbridge.domain.model.admin.Hub;

import java.util.List;
import java.util.UUID;

public interface HubRepository {

    LiveData<NetworkResultWrapper<List<Hub>>> listHubs();

    LiveData<NetworkResultWrapper<List<Hub>>> listHubs(String keyword);

    LiveData<NetworkResultWrapper<Hub>> getHubDetail(UUID hubId);

    LiveData<NetworkResultWrapper<Hub>> createHub(String name,
                                                  String address,
                                                  String phoneNumber,
                                                  String imageUrl,
                                                  String operatingHours,
                                                  Double latitude,
                                                  Double longitude);

    LiveData<NetworkResultWrapper<Hub>> updateHubStatus(UUID hubId, HubStatus status);
}
