package com.drc.aidbridge.data.remote.dto.supply;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class SupplyCategoryDto {

    @SerializedName("id")
    private String id;

    @SerializedName("name")
    private String name;

    @SerializedName("items")
    private List<SupplyItemDto> items;

    public SupplyCategoryDto() {
    }

    public SupplyCategoryDto(String id, String name, List<SupplyItemDto> items) {
        this.id = id;
        this.name = name;
        this.items = items;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<SupplyItemDto> getItems() {
        return items;
    }
}
