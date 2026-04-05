package com.drc.aidbridge.data.remote.api.volunteer;

import com.drc.aidbridge.data.remote.dto.response.volunteer.VolunteerProfileResponse;

import retrofit2.Call;
import retrofit2.http.GET;

public interface VolunteerApiService {

    @GET("volunteers/profile")
    Call<VolunteerProfileResponse> getVolunteerProfile();
}
