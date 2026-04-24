package com.drc.aidbridge.data.remote.dto.request.sponsor;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class CreateDonationRequest {

    @SerializedName("hubId")
    private final String hubId;

    @SerializedName(value = "notes")
    private final String notes;

    @SerializedName(value = "items")
    private final List<CreateDonationItemRequest> items;

    public CreateDonationRequest(String hubId, String notes, List<CreateDonationItemRequest> items) {
        this.hubId = hubId;
        this.notes = notes;
        this.items = items;
    }

    public String getHubId() {
        return hubId;
    }

    public String getNotes() {
        return notes;
    }

    public List<CreateDonationItemRequest> getItems() {
        return items;
    }
}
