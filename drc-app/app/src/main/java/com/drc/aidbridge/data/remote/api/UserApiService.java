package com.drc.aidbridge.data.remote.api;

import com.drc.aidbridge.data.remote.dto.request.ChangePasswordRequest;
import com.drc.aidbridge.data.remote.dto.request.UpdateProfileRequest;
import com.drc.aidbridge.data.remote.dto.response.AvatarUploadResponse;
import com.drc.aidbridge.data.remote.dto.response.BaseResponse;
import com.drc.aidbridge.data.remote.dto.response.UserDto;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;

public interface UserApiService {

    @PUT("user/profile")
    Call<BaseResponse<UserDto>> updateProfile(@Body UpdateProfileRequest request);

    @PUT("user/password")
    Call<BaseResponse<String>> changePassword(@Body ChangePasswordRequest request);

    @Multipart
    @POST("user/avatar")
    Call<BaseResponse<AvatarUploadResponse>> uploadAvatar(@Part MultipartBody.Part avatar);
}
