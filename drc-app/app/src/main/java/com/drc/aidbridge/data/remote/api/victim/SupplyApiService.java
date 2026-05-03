package com.drc.aidbridge.data.remote.api.victim;

import com.drc.aidbridge.data.remote.dto.response.BaseResponse;
import com.drc.aidbridge.data.remote.dto.request.victim.ReliefRequest;
import com.drc.aidbridge.data.remote.dto.request.victim.UpdateRequestLocationRequest;
import com.drc.aidbridge.data.remote.dto.response.victim.SupplyCategoryResponse;

import java.util.List;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Multipart;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Part;

public interface SupplyApiService {

    @GET("aid-requests/categories")
    Call<BaseResponse<List<SupplyCategoryResponse>>> getSupplyCategories();

    @POST("aid-requests")
    Call<BaseResponse<Object>> submitReliefRequest(@Body ReliefRequest request);

    @Multipart
    @POST("aid-requests/voice")
    Call<BaseResponse<Object>> submitVoiceReliefRequest(@Part MultipartBody.Part audio,
                                                        @Part("lat") RequestBody lat,
                                                        @Part("lng") RequestBody lng,
                                                        @Part("address") RequestBody address,
                                                        @Part("adultsCount") RequestBody adultsCount,
                                                        @Part("elderlyCount") RequestBody elderlyCount,
                                                        @Part("childrenCount") RequestBody childrenCount,
                                                        @Part("notes") RequestBody notes,
                                                        @Part("urgencyLevel") RequestBody urgencyLevel);

    @POST("aid-requests/{id}/location")
    Call<BaseResponse<Object>> updateAidRequestLocation(@Path("id") String aidRequestId,
                                                        @Body UpdateRequestLocationRequest request);
}
