package com.drc.aidbridge.data.remote.dto.response;

import com.google.gson.annotations.SerializedName;

public class AuthResponse {

    @SerializedName(value = "access_token", alternate = {"accessToken"})
    private String accessToken;

    @SerializedName(value = "refresh_token", alternate = {"refreshToken"})
    private String refreshToken;

    @SerializedName(value = "expires_in", alternate = {"expiresIn"})
    private Integer expiresIn;

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

    public Integer getExpiresIn() {
        return expiresIn;
    }

    public UserDto getUser() {
        return user;
    }
}
