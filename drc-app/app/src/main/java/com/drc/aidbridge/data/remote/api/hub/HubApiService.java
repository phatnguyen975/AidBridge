package com.drc.aidbridge.data.remote.api.hub;

import com.drc.aidbridge.data.remote.dto.response.BaseResponse;
import com.drc.aidbridge.data.remote.dto.response.hub.HubDto;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface HubApiService {

    @GET("hubs/near")
    Call<BaseResponse<List<HubDto>>> getHubsNearLocation(
            @Query("status") String status,
            @Query("lat") double lat,
            @Query("lon") double lon,
            @Query("radius") double radius
    );

}
