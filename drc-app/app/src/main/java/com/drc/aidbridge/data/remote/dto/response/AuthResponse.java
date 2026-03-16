package com.drc.aidbridge.data.remote.dto.response;

import com.google.gson.annotations.SerializedName;

public class AuthResponse {

    @SerializedName("accessToken")
    private String accessToken;

    @SerializedName("refreshToken")
    private String refreshToken;

    @SerializedName("user")
    private UserDto user;

    public AuthResponse() {
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public UserDto getUser() {
        return user;
    }
}
