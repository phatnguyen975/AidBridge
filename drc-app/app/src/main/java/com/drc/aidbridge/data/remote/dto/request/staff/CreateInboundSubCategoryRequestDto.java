package com.drc.aidbridge.data.remote.dto.request.staff;

import com.google.gson.annotations.SerializedName;

public class CreateInboundSubCategoryRequestDto {

    @SerializedName("donationId")
    private final String donationId;

    @SerializedName("parentCategoryId")
    private final String parentCategoryId;

    @SerializedName("name")
    private final String name;

    @SerializedName("unit")
    private final String unit;

    @SerializedName("iconUrl")
    private final String iconUrl;

    public CreateInboundSubCategoryRequestDto(String donationId,
                                             String parentCategoryId,
                                             String name,
                                             String unit,
                                             String iconUrl) {
        this.donationId = donationId;
        this.parentCategoryId = parentCategoryId;
        this.name = name;
        this.unit = unit;
        this.iconUrl = iconUrl;
    }
}
