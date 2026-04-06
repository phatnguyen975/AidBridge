package com.drc.aidbridge.data.remote.api.volunteer;

import com.drc.aidbridge.data.remote.dto.request.volunteer.ToggleStatusRequest;
import com.drc.aidbridge.data.remote.dto.response.volunteer.ToggleStatusResponse;
import com.drc.aidbridge.data.remote.dto.response.volunteer.VolunteerProfileResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

public interface VolunteerApiService {

    @GET("volunteers/profile")
    Call<VolunteerProfileResponse> getVolunteerProfile();

    @POST("volunteers/status")
    Call<ToggleStatusResponse> toggleVolunteerStatus(@Body ToggleStatusRequest request);
}
