package com.drc.aidbridge.domain.model.staff;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

public class InventoryQrPreview {

    private final String type;
    private final String donationId;
    private final String donationCode;
    private final String missionId;
    private final String missionCode;
    private final String status;
    private final String hubId;
    private final String hubName;
    private final List<InventoryQrPreviewItem> items;
    private final boolean canConfirm;
    private final OutboundAidRequestDetail aidRequestDetail;
    private final String message;

    public InventoryQrPreview(String type,
                              String donationId,
                              String donationCode,
                              String missionId,
                              String missionCode,
                              String status,
                              String hubId,
                              String hubName,
                              List<InventoryQrPreviewItem> items,
                              boolean canConfirm,
                              OutboundAidRequestDetail aidRequestDetail,
                              String message) {
        this.type = safeText(type);
        this.donationId = safeText(donationId);
        this.donationCode = safeText(donationCode);
        this.missionId = safeText(missionId);
        this.missionCode = safeText(missionCode);
        this.status = safeText(status);
        this.hubId = safeText(hubId);
        this.hubName = safeText(hubName);
        this.items = items != null ? items : new ArrayList<>();
        this.canConfirm = canConfirm;
        this.aidRequestDetail = aidRequestDetail;
        this.message = safeText(message);
    }

    @NonNull
    public String getType() {
        return type;
    }

    @NonNull
    public String getDonationId() {
        return donationId;
    }

    @NonNull
    public String getDonationCode() {
        return donationCode;
    }

    @NonNull
    public String getMissionId() {
        return missionId;
    }

    @NonNull
    public String getMissionCode() {
        return missionCode;
    }

    @NonNull
    public String getStatus() {
        return status;
    }

    @NonNull
    public String getHubId() {
        return hubId;
    }

    @NonNull
    public String getHubName() {
        return hubName;
    }

    @NonNull
    public List<InventoryQrPreviewItem> getItems() {
        return items;
    }

    public boolean canConfirm() {
        return canConfirm;
    }

    public OutboundAidRequestDetail getAidRequestDetail() {
        return aidRequestDetail;
    }

    @NonNull
    public String getMessage() {
        return message;
    }

    public String getDisplayCode() {
        return !donationCode.isEmpty() ? donationCode : missionCode;
    }

    private String safeText(String value) {
        return value == null ? "" : value.trim();
    }
}
