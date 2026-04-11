package com.drc.aidbridge.data.remote.dto.response.victim;

import com.google.gson.annotations.SerializedName;

public class SupplyItemResponse {

    @SerializedName("id")
    private String id;

    @SerializedName("name")
    private String name;

    public SupplyItemResponse() {
    }

    public SupplyItemResponse(String id, String name) {
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
