package com.drc.aidbridge.data.remote.dto.response.staff;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class SearchInboundSubCategoriesResponseDto {

    @SerializedName("parentCategoryId")
    private String parentCategoryId;

    @SerializedName("parentCategoryName")
    private String parentCategoryName;

    @SerializedName("items")
    private List<InboundSubCategoryDto> items;

    public String getParentCategoryId() {
        return parentCategoryId;
    }

    public String getParentCategoryName() {
        return parentCategoryName;
    }

    public List<InboundSubCategoryDto> getItems() {
        return items;
    }
}
