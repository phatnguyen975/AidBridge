package com.drc.aidbridge.data.remote.api.gateway;

import com.drc.aidbridge.data.remote.dto.request.gateway.GatewaySmsSosRequest;
import com.drc.aidbridge.data.remote.dto.response.BaseResponse;
import com.drc.aidbridge.data.remote.dto.response.victim.SosRequestResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface SmsIngestApiService {

    @POST("sms-ingest/sos")
    Call<BaseResponse<SosRequestResponse>> ingestSmsSos(
        @Header("X-Gateway-Token") String gatewayToken,
        @Body GatewaySmsSosRequest request
    );
}
