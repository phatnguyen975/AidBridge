package com.drc.aidbridge.data.remote.api;

import com.drc.aidbridge.data.remote.dto.response.BaseResponse;
import com.drc.aidbridge.data.remote.dto.response.PaginatedData;
import com.drc.aidbridge.data.remote.dto.response.victim.VictimHistoryDto;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface VictimHistoryApiService {

    @GET("victim/history")
    Call<BaseResponse<PaginatedData<VictimHistoryDto>>> getVictimHistory(
        @Query("page") int page,
        @Query("size") int size,
        @Query("timeRange") String timeRange
    );
}
