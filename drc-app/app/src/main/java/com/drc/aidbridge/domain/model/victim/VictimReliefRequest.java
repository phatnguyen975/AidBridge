package com.drc.aidbridge.domain.model.victim;

import java.util.Collections;
import java.util.List;

public class VictimReliefRequest {

    private final Double latitude;
    private final Double longitude;
    private final String address;
    private final int adultsCount;
    private final int eldersCount;
    private final int childrenCount;
    private final String note;
    private final List<VictimRequestedItem> requestedItems;

    public VictimReliefRequest(Double latitude,
                               Double longitude,
                               String address,
                               int adultsCount,
                               int eldersCount,
                               int childrenCount,
                               String note,
                               List<VictimRequestedItem> requestedItems) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.address = address != null ? address.trim() : "";
        this.adultsCount = Math.max(0, adultsCount);
        this.eldersCount = Math.max(0, eldersCount);
        this.childrenCount = Math.max(0, childrenCount);
        this.note = note != null ? note.trim() : "";
        this.requestedItems = requestedItems != null ? requestedItems : Collections.emptyList();
    }

    public Double getLatitude() {
        return latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public String getAddress() {
        return address;
    }

    public int getAdultsCount() {
        return adultsCount;
    }

    public int getEldersCount() {
        return eldersCount;
    }

    public int getChildrenCount() {
        return childrenCount;
    }

    public String getNote() {
        return note;
    }

    public List<VictimRequestedItem> getRequestedItems() {
        return requestedItems;
    }
}
