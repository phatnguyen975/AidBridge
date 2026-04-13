package com.drc.aidbridge.data.remote.api;

import com.drc.aidbridge.data.remote.dto.request.AcceptMissionRequest;
import com.drc.aidbridge.data.remote.dto.request.RejectMissionRequest;
import com.drc.aidbridge.data.remote.dto.response.BaseResponse;
import com.drc.aidbridge.data.remote.dto.response.MissionDto;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface MissionApiService {

    @GET("missions/{id}")
    Call<BaseResponse<MissionDto>> getMission(@Path("id") String missionId);

    @POST("missions/{id}/accept")
    Call<BaseResponse<MissionDto>> acceptMission(@Path("id") String missionId,
                                                 @Body AcceptMissionRequest request);

    @POST("missions/{id}/reject")
    Call<BaseResponse<Void>> rejectMission(@Path("id") String missionId,
                                           @Body RejectMissionRequest request);
}
