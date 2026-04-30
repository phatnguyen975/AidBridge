package com.drc.aidbridge.data.remote.api.volunteer;

import com.drc.aidbridge.data.remote.dto.request.volunteer.AcceptDispatchAttemptRequest;
import com.drc.aidbridge.data.remote.dto.request.volunteer.CancelDispatchAttemptRequest;
import com.drc.aidbridge.data.remote.dto.request.volunteer.PingVolunteerHeartbeatRequest;
import com.drc.aidbridge.data.remote.dto.request.volunteer.ToggleStatusRequest;
import com.drc.aidbridge.data.remote.dto.request.volunteer.CompleteMissionRequestDto;
import com.drc.aidbridge.data.remote.dto.request.volunteer.CancelMissionRequestDto;
import com.drc.aidbridge.data.remote.dto.response.volunteer.ToggleStatusResponse;
import com.drc.aidbridge.data.remote.dto.response.volunteer.VolunteerHistoryResponseDto;
import com.drc.aidbridge.data.remote.dto.response.volunteer.VolunteerProfileResponse;
import com.drc.aidbridge.data.remote.dto.response.volunteer.LatestDispatchResponse;
import com.drc.aidbridge.data.remote.dto.response.volunteer.MissionHistoryFullResponseDto;
import com.drc.aidbridge.data.remote.dto.response.volunteer.CurrentMissionResponseDto;
import com.drc.aidbridge.data.remote.dto.response.volunteer.CompleteMissionResponseDto;
import com.drc.aidbridge.data.remote.dto.response.volunteer.CancelMissionResponseDto;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface VolunteerApiService {

    @GET("volunteers/missions/history")
    Call<VolunteerHistoryResponseDto> getMissionHistory(
            @Query("page") int page,
            @Query("limit") int limit);

    @GET("volunteers/missions/history/full")
    Call<MissionHistoryFullResponseDto> getMissionHistoryFull(
            @Query("page") int page,
            @Query("limit") int limit);

    @GET("volunteers/missions/current")
    Call<CurrentMissionResponseDto> getCurrentMission();

    @POST("volunteers/missions/complete")
    Call<CompleteMissionResponseDto> completeMission(@Body CompleteMissionRequestDto request);

    @POST("volunteers/missions/cancel")
    Call<CancelMissionResponseDto> cancelMission(@Body CancelMissionRequestDto request);

    @GET("volunteers/profile")
    Call<VolunteerProfileResponse> getVolunteerProfile();

    @POST("volunteers/status")
    Call<ToggleStatusResponse> toggleVolunteerStatus(@Body ToggleStatusRequest request);

    @POST("volunteers/ping")
    Call<VolunteerProfileResponse> pingVolunteerHeartbeat(@Body PingVolunteerHeartbeatRequest request);

    @GET("volunteers/missions/dispatch/latest")
    Call<LatestDispatchResponse> getLatestDispatch();

    @PATCH("volunteers/missions/dispatch/cancel")
    Call<LatestDispatchResponse> cancelDispatchAttempt(@Body CancelDispatchAttemptRequest request);

    @PATCH("volunteers/missions/dispatch/accept")
    Call<LatestDispatchResponse> acceptDispatchAttempt(@Body AcceptDispatchAttemptRequest request);
}
