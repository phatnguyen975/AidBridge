package com.drc.aidbridge.data.remote.dto.request.victim;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class ReliefRequest {

    @SerializedName("adultsCount")
    private final int adultsCount;

    @SerializedName("eldersCount")
    private final int eldersCount;

    @SerializedName("childrenCount")
    private final int childrenCount;

    @SerializedName("note")
    private final String note;

    @SerializedName("requestedItems")
    private final List<RequestedItemRequest> requestedItems;

    public ReliefRequest(int adultsCount,
                         int eldersCount,
                         int childrenCount,
                         String note,
                         List<RequestedItemRequest> requestedItems) {
        this.adultsCount = adultsCount;
        this.eldersCount = eldersCount;
        this.childrenCount = childrenCount;
        this.note = note;
        this.requestedItems = requestedItems;
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

    public List<RequestedItemRequest> getRequestedItems() {
        return requestedItems;
    }
}
