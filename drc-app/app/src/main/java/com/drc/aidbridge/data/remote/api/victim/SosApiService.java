package com.drc.aidbridge.data.remote.api.victim;

import com.drc.aidbridge.data.remote.dto.request.victim.CreateSosRequest;
import com.drc.aidbridge.data.remote.dto.response.BaseResponse;
import com.drc.aidbridge.data.remote.dto.response.victim.SosRequestResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface SosApiService {

    @POST("sos-requests")
    Call<BaseResponse<SosRequestResponse>> createSosRequest(@Body CreateSosRequest request);
}
