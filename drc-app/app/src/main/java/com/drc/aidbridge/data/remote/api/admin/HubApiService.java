package com.drc.aidbridge.data.remote.api.admin;

import com.drc.aidbridge.data.remote.dto.request.admin.CreateHubRequest;
import com.drc.aidbridge.data.remote.dto.request.admin.UpdateHubStatusRequest;
import com.drc.aidbridge.data.remote.dto.response.BaseResponse;
import com.drc.aidbridge.data.remote.dto.response.admin.HubResponseDto;

import java.util.List;
import java.util.UUID;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface HubApiService {

    @GET("hubs")
    Call<BaseResponse<List<HubResponseDto>>> getHubs(@Query("keyword") String keyword);

    @GET("hubs/{id}")
    Call<BaseResponse<HubResponseDto>> getHubById(@Path("id") UUID hubId);

    @POST("hubs")
    Call<BaseResponse<HubResponseDto>> createHub(@Body CreateHubRequest request);

    @PATCH("hubs/{id}")
    Call<BaseResponse<HubResponseDto>> updateHubStatus(
            @Path("id") UUID hubId,
            @Body UpdateHubStatusRequest request);
}
