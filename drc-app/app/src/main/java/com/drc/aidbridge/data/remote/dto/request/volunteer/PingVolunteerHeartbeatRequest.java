package com.drc.aidbridge.data.remote.dto.request.volunteer;

import com.google.gson.annotations.SerializedName;

public class PingVolunteerHeartbeatRequest {

    @SerializedName("lat")
    private final double lat;

    @SerializedName("lng")
    private final double lng;

    public PingVolunteerHeartbeatRequest(double lat, double lng) {
        this.lat = lat;
        this.lng = lng;
    }

    public double getLat() {
        return lat;
    }

    public double getLng() {
        return lng;
    }
}
