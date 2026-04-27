package com.drc.aidbridge.domain.model.sponsor;

import java.util.List;

public class SponsorDonationRequest {

    private final String hubId;
    private final List<SponsorDonationItem> items;

    public SponsorDonationRequest(String hubId,
                                  List<SponsorDonationItem> items) {
        this.hubId = hubId;
        this.items = items;
    }

    public String getHubId() {
        return hubId;
    }

    public List<SponsorDonationItem> getItems() {
        return items;
    }
}
