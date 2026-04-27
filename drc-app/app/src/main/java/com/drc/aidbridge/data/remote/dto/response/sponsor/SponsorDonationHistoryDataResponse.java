package com.drc.aidbridge.data.remote.dto.response.sponsor;

import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class SponsorDonationHistoryDataResponse {

    @Nullable
    @SerializedName("items")
    private List<SponsorDonationHistoryItemResponse> items;

    @Nullable
    @SerializedName("pagination")
    private SponsorDonationHistoryPaginationResponse pagination;

    @Nullable
    public List<SponsorDonationHistoryItemResponse> getItems() {
        return items;
    }

    @Nullable
    public SponsorDonationHistoryPaginationResponse getPagination() {
        return pagination;
    }
}
