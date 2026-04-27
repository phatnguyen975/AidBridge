package com.drc.aidbridge.data.remote.api.victim;

import com.drc.aidbridge.data.remote.dto.request.victim.CreateSosRequest;
import com.drc.aidbridge.data.remote.dto.request.victim.UpdateRequestLocationRequest;
import com.drc.aidbridge.data.remote.dto.response.BaseResponse;
import com.drc.aidbridge.data.remote.dto.response.victim.SosRequestResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface SosApiService {

    @POST("sos-requests")
    Call<BaseResponse<SosRequestResponse>> createSosRequest(@Body CreateSosRequest request);

    @POST("sos-requests/{id}/location")
    Call<BaseResponse<Object>> updateSosLocation(@Path("id") String sosRequestId,
                                                 @Body UpdateRequestLocationRequest request);
}
