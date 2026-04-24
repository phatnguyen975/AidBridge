package com.drc.aidbridge.domain.model.sponsor;

import java.util.List;

public class SponsorDonationRequest {

    private final String hubId;
    private final String notes;
    private final List<SponsorDonationItem> items;

    public SponsorDonationRequest(String hubId,
                                  String notes,
                                  List<SponsorDonationItem> items) {
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

    public List<SponsorDonationItem> getItems() {
        return items;
    }
}
