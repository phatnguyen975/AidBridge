package com.drc.aidbridge.data.remote.dto.request.sponsor;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class CreateDonationRequest {

    @SerializedName("hubId")
    private final String hubId;

    @SerializedName(value = "items")
    private final List<CreateDonationItemRequest> items;

    public CreateDonationRequest(String hubId, List<CreateDonationItemRequest> items) {
        this.hubId = hubId;
        this.items = items;
    }

    public String getHubId() {
        return hubId;
    }

    public List<CreateDonationItemRequest> getItems() {
        return items;
    }
}
