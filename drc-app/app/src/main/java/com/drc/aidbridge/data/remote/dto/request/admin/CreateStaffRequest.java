package com.drc.aidbridge.data.remote.dto.request.admin;

import com.google.gson.annotations.SerializedName;

public class CreateStaffRequest {

    @SerializedName("fullName")
    private final String fullName;

    @SerializedName("email")
    private final String email;

    @SerializedName("phoneNumber")
    private final String phoneNumber;

    @SerializedName("password")
    private final String password;

    @SerializedName("hubId")
    private final String hubId;

    public CreateStaffRequest(String fullName,
                              String email,
                              String phoneNumber,
                              String password,
                              String hubId) {
        this.fullName = fullName;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.password = password;
        this.hubId = hubId;
    }
}
