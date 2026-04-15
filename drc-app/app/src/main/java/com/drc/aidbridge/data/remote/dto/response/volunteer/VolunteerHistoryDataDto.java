package com.drc.aidbridge.data.remote.dto.response.volunteer;

import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class VolunteerHistoryDataDto {

    @Nullable
    @SerializedName("items")
    private List<VolunteerHistoryItemDto> items;

    @Nullable
    @SerializedName("pagination")
    private VolunteerHistoryPaginationDto pagination;

    @Nullable
    public List<VolunteerHistoryItemDto> getItems() {
        return items;
    }

    @Nullable
    public VolunteerHistoryPaginationDto getPagination() {
        return pagination;
    }
}