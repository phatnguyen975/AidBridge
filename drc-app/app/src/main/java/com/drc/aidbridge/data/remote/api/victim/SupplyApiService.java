package com.drc.aidbridge.data.remote.api.victim;

import com.drc.aidbridge.data.remote.dto.response.BaseResponse;
import com.drc.aidbridge.data.remote.dto.request.victim.ReliefRequest;
import com.drc.aidbridge.data.remote.dto.response.victim.SupplyCategoryResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

public interface SupplyApiService {

    @GET("supplies/categories")
    Call<BaseResponse<List<SupplyCategoryResponse>>> getSupplyCategories();

    @POST("relief-requests")
    Call<BaseResponse<String>> submitReliefRequest(@Body ReliefRequest request);
}
