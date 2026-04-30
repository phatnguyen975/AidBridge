package com.drc.aidbridge.data.remote.dto.response.volunteer;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class MissionHistoryFullDataDto {
    @SerializedName("items")
    private List<MissionHistoryFullItemDto> items;

    @SerializedName("pagination")
    private VolunteerHistoryPaginationDto pagination;

    public List<MissionHistoryFullItemDto> getItems() {
        return items;
    }

    public VolunteerHistoryPaginationDto getPagination() {
        return pagination;
    }
}
