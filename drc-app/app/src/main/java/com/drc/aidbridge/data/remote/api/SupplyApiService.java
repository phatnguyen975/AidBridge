package com.drc.aidbridge.data.remote.api;

import com.drc.aidbridge.data.remote.dto.response.BaseResponse;
import com.drc.aidbridge.data.remote.dto.supply.ReliefRequestDto;
import com.drc.aidbridge.data.remote.dto.supply.SupplyCategoryDto;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

public interface SupplyApiService {

    @GET("supplies/categories")
    Call<BaseResponse<List<SupplyCategoryDto>>> getSupplyCategories();

    @POST("relief-requests")
    Call<BaseResponse<String>> submitReliefRequest(@Body ReliefRequestDto requestDto);
}
