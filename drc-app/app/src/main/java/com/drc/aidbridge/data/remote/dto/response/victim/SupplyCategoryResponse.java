package com.drc.aidbridge.data.remote.dto.response.victim;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class SupplyCategoryResponse {

    @SerializedName("id")
    private String id;

    @SerializedName("name")
    private String name;

    @SerializedName("items")
    private List<SupplyItemResponse> items;

    public SupplyCategoryResponse() {
    }

    public SupplyCategoryResponse(String id, String name, List<SupplyItemResponse> items) {
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

    public List<SupplyItemResponse> getItems() {
        return items;
    }
}
