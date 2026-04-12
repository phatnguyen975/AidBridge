package com.drc.aidbridge.data.remote.dto.response.victim;

import com.google.gson.annotations.SerializedName;

public class HistoryDetailItemResponse {

    @SerializedName(value = "categoryName", alternate = {"category_name", "name"})
    private String categoryName;

    @SerializedName(value = "quantity", alternate = {"qty"})
    private Integer quantity;

    @SerializedName(value = "unit", alternate = {"unit_name"})
    private String unit;

    public String getCategoryName() {
        return categoryName;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public String getUnit() {
        return unit;
    }
}
