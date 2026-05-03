package com.drc.aidbridge.data.remote.dto.request.admin;

import com.google.gson.annotations.SerializedName;

public class CreateHubRequest {

    @SerializedName("name")
    private final String name;

    @SerializedName("address")
    private final String address;

    @SerializedName("phoneNumber")
    private final String phoneNumber;

    @SerializedName("imageUrl")
    private final String imageUrl;

    @SerializedName("status")
    private final String status;

    @SerializedName("operatingHours")
    private final String operatingHours;

    @SerializedName("lat")
    private final Double latitude;

    @SerializedName("lng")
    private final Double longitude;

    public CreateHubRequest(String name,
                            String address,
                            String phoneNumber,
                            String imageUrl,
                            String status,
                            String operatingHours,
                            Double latitude,
                            Double longitude) {
        this.name = name;
        this.address = address;
        this.phoneNumber = phoneNumber;
        this.imageUrl = imageUrl;
        this.status = status;
        this.operatingHours = operatingHours;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getStatus() {
        return status;
    }

    public String getOperatingHours() {
        return operatingHours;
    }

    public Double getLatitude() {
        return latitude;
    }

    public Double getLongitude() {
        return longitude;
    }
}
