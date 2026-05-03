package com.drc.aidbridge.data.remote.api.admin;

import com.drc.aidbridge.data.remote.dto.response.BaseResponse;
import com.drc.aidbridge.data.remote.dto.response.admin.AdminDashboardSummaryResponseDto;

import com.drc.aidbridge.data.remote.dto.response.admin.AdminRoutingSosAidResponseDto;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface AdminDashboardApiService {

    @GET("admin/dashboard/summary")
    Call<BaseResponse<AdminDashboardSummaryResponseDto>> getSummary();

    @GET("admin/routing/sos-aid")
    Call<AdminRoutingSosAidResponseDto> getSosAidRequests(
            @Query("status") String status,
            @Query("startDate") String startDate,
            @Query("endDate") String endDate
    );
}
