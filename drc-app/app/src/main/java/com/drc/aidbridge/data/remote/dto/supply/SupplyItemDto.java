package com.drc.aidbridge.data.remote.dto.supply;

import com.google.gson.annotations.SerializedName;

public class SupplyItemDto {

    @SerializedName("id")
    private String id;

    @SerializedName("name")
    private String name;

    public SupplyItemDto() {
    }

    public SupplyItemDto(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
