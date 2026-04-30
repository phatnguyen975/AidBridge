package com.drc.aidbridge.data.remote.api.admin;

import com.drc.aidbridge.data.remote.dto.response.BaseResponse;
import com.drc.aidbridge.data.remote.dto.response.admin.AdminDashboardSummaryResponseDto;

import retrofit2.Call;
import retrofit2.http.GET;

public interface AdminDashboardApiService {

    @GET("admin/dashboard/summary")
    Call<BaseResponse<AdminDashboardSummaryResponseDto>> getSummary();
}
