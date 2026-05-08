package com.drc.aidbridge.data.remote.api.staff;

import com.drc.aidbridge.data.remote.dto.response.BaseResponse;
import com.drc.aidbridge.data.remote.dto.response.staff.StaffUpcomingDeliveryMissionDto;
import com.drc.aidbridge.data.remote.dto.response.staff.StaffUpcomingDonationDto;
import com.drc.aidbridge.data.remote.dto.response.staff.StaffProfileDto;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface StaffApiService {

    @GET("staff/by-user/{userId}")
    Call<BaseResponse<StaffProfileDto>> getStaffByUser(@Path("userId") String userId);

    @GET("staff/tasks/upcoming/deliveries")
    Call<BaseResponse<List<StaffUpcomingDeliveryMissionDto>>> getUpcomingDeliveryMissions(
            @Query("page") int page,
            @Query("limit") int limit
    );

    @GET("staff/tasks/upcoming/donations")
    Call<BaseResponse<List<StaffUpcomingDonationDto>>> getUpcomingDonations(
            @Query("page") int page,
            @Query("limit") int limit
    );
}
