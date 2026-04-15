package com.drc.aidbridge.data.remote.api.admin;

import com.drc.aidbridge.data.remote.dto.request.admin.UpdateHubStatusRequest;
import com.drc.aidbridge.data.remote.dto.response.BaseResponse;
import com.drc.aidbridge.data.remote.dto.response.admin.HubResponseDto;

import java.util.List;
import java.util.UUID;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.Path;

public interface HubApiService {

    @GET("hubs")
    Call<BaseResponse<List<HubResponseDto>>> getHubs();

    @PATCH("hubs/{id}")
    Call<BaseResponse<HubResponseDto>> updateHubStatus(
            @Path("id") UUID hubId,
            @Body UpdateHubStatusRequest request);
}
