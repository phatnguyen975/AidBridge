package com.drc.aidbridge.data.remote.dto.response.staff;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class OutboundAidRequestDetailDto {

    @SerializedName("id")
    private String id;

    @SerializedName("description")
    private String description;

    @SerializedName("numberAdult")
    private Integer numberAdult;

    @SerializedName("numberElderly")
    private Integer numberElderly;

    @SerializedName("numberChildren")
    private Integer numberChildren;

    @SerializedName("items")
    private List<OutboundAidRequestItemDto> items;

    public String getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public Integer getNumberAdult() {
        return numberAdult;
    }

    public Integer getNumberElderly() {
        return numberElderly;
    }

    public Integer getNumberChildren() {
        return numberChildren;
    }

    public List<OutboundAidRequestItemDto> getItems() {
        return items;
    }
}
