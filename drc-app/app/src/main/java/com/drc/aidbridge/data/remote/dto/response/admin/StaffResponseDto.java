package com.drc.aidbridge.data.remote.dto.response.admin;

import com.google.gson.annotations.SerializedName;

public class StaffResponseDto {

    @SerializedName("id")
    private String id;

    @SerializedName(value = "user_id", alternate = { "userId" })
    private String userId;

    @SerializedName(value = "full_name", alternate = { "fullName" })
    private String fullName;

    @SerializedName("email")
    private String email;

    @SerializedName(value = "phone_number", alternate = { "phoneNumber" })
    private String phoneNumber;

    @SerializedName(value = "hub_id", alternate = { "hubId" })
    private String hubId;

    @SerializedName(value = "hub_name", alternate = { "hubName" })
    private String hubName;

    public String getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getHubId() {
        return hubId;
    }

    public String getHubName() {
        return hubName;
    }
}
