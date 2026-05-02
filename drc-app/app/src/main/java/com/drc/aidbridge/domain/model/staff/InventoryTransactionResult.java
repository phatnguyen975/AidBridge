package com.drc.aidbridge.domain.model.staff;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

public class InventoryTransactionResult {

    private final String message;
    private final String donationId;
    private final String donationCode;
    private final String missionId;
    private final List<InventoryTransactionItem> updatedItems;

    public InventoryTransactionResult(String message,
                                      String donationId,
                                      String donationCode,
                                      String missionId,
                                      List<InventoryTransactionItem> updatedItems) {
        this.message = safeText(message);
        this.donationId = safeText(donationId);
        this.donationCode = safeText(donationCode);
        this.missionId = safeText(missionId);
        this.updatedItems = updatedItems != null ? updatedItems : new ArrayList<>();
    }

    @NonNull
    public String getMessage() {
        return message;
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
    public List<InventoryTransactionItem> getUpdatedItems() {
        return updatedItems;
    }

    private String safeText(String value) {
        return value == null ? "" : value.trim();
    }
}
