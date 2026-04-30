package com.drc.aidbridge.data.remote.api.staff;

import com.drc.aidbridge.data.remote.dto.response.BaseResponse;
import com.drc.aidbridge.data.remote.dto.response.staff.StaffInventoryResponseDto;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface StaffInventoryApiService {

    @GET("staff/inventory")
    Call<BaseResponse<StaffInventoryResponseDto>> getMyHubInventory(
            @Query("parentCategoryId") String parentCategoryId,
            @Query("parentCategoryName") String parentCategoryName,
            @Query("keyword") String keyword,
            @Query("page") int page,
            @Query("size") int size
    );
}
