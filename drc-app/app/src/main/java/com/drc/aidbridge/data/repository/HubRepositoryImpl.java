package com.drc.aidbridge.data.repository;

import com.drc.aidbridge.data.remote.api.hub.HubApiService;
import com.drc.aidbridge.data.remote.dto.response.BaseResponse;
import com.drc.aidbridge.data.remote.dto.response.hub.HubDto;
import com.drc.aidbridge.domain.repository.HubRepository;

import java.util.List;

import javax.inject.Inject;

import retrofit2.Call;

public class HubRepositoryImpl implements HubRepository {

    private final HubApiService hubApiService;

    @Inject
    public HubRepositoryImpl(HubApiService hubApiService) {
        this.hubApiService = hubApiService;
    }

    @Override
    public Call<BaseResponse<List<HubDto>>> getHubsNearLocation(String status, double lat, double lon, double radius) {
        return hubApiService.getHubsNearLocation(status, lat, lon, radius);
    }
}
