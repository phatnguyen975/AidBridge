package com.drc.aidbridge.data.remote.dto.response.admin;

import com.google.gson.annotations.SerializedName;

public class HubResponseDto {

    @SerializedName("id")
    private String id;

    @SerializedName("name")
    private String name;

    @SerializedName("address")
    private String address;

    @SerializedName("status")
    private String status;

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }

    public String getStatus() {
        return status;
    }
}
