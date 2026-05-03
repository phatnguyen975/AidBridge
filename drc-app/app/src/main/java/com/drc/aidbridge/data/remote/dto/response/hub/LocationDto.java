package com.drc.aidbridge.data.remote.dto.response.hub;

import com.google.gson.annotations.SerializedName;
import java.util.UUID;

public class LocationDto {
    @SerializedName("lat")
    private double lat;

    @SerializedName("lng")
    private double lng;

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLng() {
        return lng;
    }

    public void setLng(double lng) {
        this.lng = lng;
    }
}
