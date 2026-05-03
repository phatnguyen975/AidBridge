package com.drc.aidbridge.data.remote.dto.response.sponsor;

import com.google.gson.annotations.SerializedName;

public class SponsorDonationHistoryPaginationResponse {

    @SerializedName("page")
    private int page;

    @SerializedName("limit")
    private int limit;

    @SerializedName("total")
    private long total;

    @SerializedName("totalPages")
    private int totalPages;

    @SerializedName("hasNext")
    private boolean hasNext;

    public int getPage() {
        return page;
    }

    public int getLimit() {
        return limit;
    }

    public long getTotal() {
        return total;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public boolean isHasNext() {
        return hasNext;
    }
}
