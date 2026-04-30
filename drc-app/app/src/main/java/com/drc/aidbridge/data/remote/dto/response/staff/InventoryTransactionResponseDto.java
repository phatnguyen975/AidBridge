package com.drc.aidbridge.data.remote.dto.response.staff;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class InventoryTransactionResponseDto {

    @SerializedName("message")
    private String message;

    @SerializedName("donationId")
    private String donationId;

    @SerializedName("donationCode")
    private String donationCode;

    @SerializedName("missionId")
    private String missionId;

    @SerializedName("updatedItems")
    private List<InventoryTransactionItemDto> updatedItems;

    public String getMessage() {
        return message;
    }

    public String getDonationId() {
        return donationId;
    }

    public String getDonationCode() {
        return donationCode;
    }

    public String getMissionId() {
        return missionId;
    }

    public List<InventoryTransactionItemDto> getUpdatedItems() {
        return updatedItems;
    }
}
