package com.drc.aidbridge.data.remote.api.staff;

import com.drc.aidbridge.data.remote.dto.response.BaseResponse;
import com.drc.aidbridge.data.remote.dto.response.staff.StaffProfileDto;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface StaffApiService {

    @GET("staff/by-user/{userId}")
    Call<BaseResponse<StaffProfileDto>> getStaffByUser(@Path("userId") String userId);
}
