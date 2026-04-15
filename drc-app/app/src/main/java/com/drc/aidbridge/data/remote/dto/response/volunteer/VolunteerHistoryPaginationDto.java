package com.drc.aidbridge.data.remote.dto.response.volunteer;

import com.google.gson.annotations.SerializedName;

public class VolunteerHistoryPaginationDto {

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

    @SerializedName("hasPrevious")
    private boolean hasPrevious;

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

    public boolean isHasPrevious() {
        return hasPrevious;
    }
}