package com.drc.aidbridge.data.remote.api.victim;

import com.drc.aidbridge.data.remote.dto.response.BaseResponse;
import com.drc.aidbridge.data.remote.dto.response.PaginatedData;
import com.drc.aidbridge.data.remote.dto.response.victim.HistoryDetailResponse;
import com.drc.aidbridge.data.remote.dto.response.victim.HistoryResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface HistoryApiService {

    @GET("victim/history")
    Call<BaseResponse<PaginatedData<HistoryResponse>>> getVictimHistory(
        @Query("page") int page,
        @Query("size") int size,
        @Query("timeRange") String timeRange
    );

    @GET("victim/history/{requestId}/detail")
    Call<BaseResponse<HistoryDetailResponse>> getVictimHistoryDetail(
        @Path("requestId") String requestId,
        @Query("type") String type
    );
}
