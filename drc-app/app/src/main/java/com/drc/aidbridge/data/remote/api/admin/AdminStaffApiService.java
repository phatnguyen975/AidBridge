package com.drc.aidbridge.data.remote.api.admin;

import com.drc.aidbridge.data.remote.dto.request.admin.CreateStaffRequest;
import com.drc.aidbridge.data.remote.dto.response.BaseResponse;
import com.drc.aidbridge.data.remote.dto.response.admin.StaffResponseDto;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

public interface AdminStaffApiService {

    @GET("admin/staff")
    Call<BaseResponse<List<StaffResponseDto>>> getStaffList();

    @POST("admin/staff")
    Call<BaseResponse<StaffResponseDto>> createStaff(@Body CreateStaffRequest request);
}
