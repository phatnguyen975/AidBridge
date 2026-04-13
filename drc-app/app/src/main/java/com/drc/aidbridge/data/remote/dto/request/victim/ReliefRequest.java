package com.drc.aidbridge.data.remote.dto.request.victim;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class ReliefRequest {

    @SerializedName("lat")
    private final double lat;

    @SerializedName("lng")
    private final double lng;

    @SerializedName("address")
    private final String address;

    @SerializedName("adultsCount")
    private final int adultsCount;

    @SerializedName("elderlyCount")
    private final int elderlyCount;

    @SerializedName("childrenCount")
    private final int childrenCount;

    @SerializedName("notes")
    private final String notes;

    @SerializedName("items")
    private final List<RequestedItemRequest> items;

    public ReliefRequest(double lat,
                         double lng,
                         String address,
                         int adultsCount,
                         int elderlyCount,
                         int childrenCount,
                         String notes,
                         List<RequestedItemRequest> items) {
        this.lat = lat;
        this.lng = lng;
        this.address = address;
        this.adultsCount = adultsCount;
        this.elderlyCount = elderlyCount;
        this.childrenCount = childrenCount;
        this.notes = notes;
        this.items = items;
    }

    public double getLat() {
        return lat;
    }

    public double getLng() {
        return lng;
    }

    public String getAddress() {
        return address;
    }

    public int getAdultsCount() {
        return adultsCount;
    }

    public int getElderlyCount() {
        return elderlyCount;
    }

    public int getChildrenCount() {
        return childrenCount;
    }

    public String getNotes() {
        return notes;
    }

    public List<RequestedItemRequest> getItems() {
        return items;
    }
}
