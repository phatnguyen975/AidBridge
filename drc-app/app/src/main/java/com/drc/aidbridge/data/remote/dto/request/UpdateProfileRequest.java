package com.drc.aidbridge.data.remote.dto.request;

import com.google.gson.annotations.SerializedName;

public class UpdateProfileRequest {

    @SerializedName("name")
    private final String name;

    @SerializedName("phone")
    private final String phone;

    @SerializedName("address")
    private final String address;

    public UpdateProfileRequest(String name, String phone, String address) {
        this.name = name;
        this.phone = phone;
        this.address = address;
    }

    public String getName() {
        return name;
    }

    public String getPhone() {
        return phone;
    }

    public String getAddress() {
        return address;
    }
}
