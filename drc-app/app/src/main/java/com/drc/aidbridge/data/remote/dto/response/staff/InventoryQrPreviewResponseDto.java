package com.drc.aidbridge.data.remote.dto.response.staff;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class InventoryQrPreviewResponseDto {

    @SerializedName("type")
    private String type;

    @SerializedName("donationId")
    private String donationId;

    @SerializedName("donationCode")
    private String donationCode;

    @SerializedName("missionId")
    private String missionId;

    @SerializedName("missionCode")
    private String missionCode;

    @SerializedName("status")
    private String status;

    @SerializedName("hubId")
    private String hubId;

    @SerializedName("hubName")
    private String hubName;

    @SerializedName("items")
    private List<InventoryQrPreviewItemDto> items;

    @SerializedName("canConfirm")
    private Boolean canConfirm;

    @SerializedName("aidRequestDetail")
    private OutboundAidRequestDetailDto aidRequestDetail;

    @SerializedName("message")
    private String message;

    public String getType() {
        return type;
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

    public String getMissionCode() {
        return missionCode;
    }

    public String getStatus() {
        return status;
    }

    public String getHubId() {
        return hubId;
    }

    public String getHubName() {
        return hubName;
    }

    public List<InventoryQrPreviewItemDto> getItems() {
        return items;
    }

    public Boolean getCanConfirm() {
        return canConfirm;
    }

    public OutboundAidRequestDetailDto getAidRequestDetail() {
        return aidRequestDetail;
    }

    public String getMessage() {
        return message;
    }
}
