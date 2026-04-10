package com.drc.aidbridge.data.remote.api;

import com.drc.aidbridge.data.remote.dto.response.BaseResponse;

import java.util.List;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

public interface SosApiService {

    @Multipart
    @POST("sos/self")
    Call<BaseResponse<String>> uploadSelfSos(
        @Part("fullName") RequestBody fullName,
        @Part("peopleCount") RequestBody peopleCount,
        @Part("severity") RequestBody severity,
        @Part("note") RequestBody note,
        @Part("latitude") RequestBody latitude,
        @Part("longitude") RequestBody longitude,
        @Part List<MultipartBody.Part> images
    );

    @Multipart
    @POST("sos/relative")
    Call<BaseResponse<String>> uploadRelativeSos(
        @Part("relativeName") RequestBody relativeName,
        @Part("relativePhone") RequestBody relativePhone,
        @Part("relativeAddress") RequestBody relativeAddress,
        @Part("severity") RequestBody severity,
        @Part("latitude") RequestBody latitude,
        @Part("longitude") RequestBody longitude
    );
}
