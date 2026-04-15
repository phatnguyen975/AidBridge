package com.drc.aidbridge.data.remote.dto.response.admin;

import com.google.gson.annotations.SerializedName;

public class HubResponseDto {

    @SerializedName("id")
    private String id;

    @SerializedName("name")
    private String name;

    @SerializedName("address")
    private String address;

    @SerializedName(value = "image_url", alternate = { "imageUrl" })
    private String imageUrl;

    @SerializedName(value = "operating_hours", alternate = { "operatingHours" })
    private String operatingHours;

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

    public String getImageUrl() {
        return imageUrl;
    }

    public String getOperatingHours() {
        return operatingHours;
    }

    public String getStatus() {
        return status;
    }
}
