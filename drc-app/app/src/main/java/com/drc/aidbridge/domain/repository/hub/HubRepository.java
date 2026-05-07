package com.drc.aidbridge.domain.repository;

import com.drc.aidbridge.data.remote.dto.response.BaseResponse;
import com.drc.aidbridge.data.remote.dto.response.hub.HubDto;

import java.util.List;

import retrofit2.Call;

public interface HubRepository {
    Call<BaseResponse<List<HubDto>>> getHubsNearLocation(String status, double lat, double lon, double radius);
}
