package com.drc.aidbridge.domain.model.staff;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

public class InboundDonationPreview {

    private final String donationId;
    private final String donationCode;
    private final String status;
    private final String hubName;
    private final String message;
    private final List<InboundParentCategory> registeredParentCategories;

    public InboundDonationPreview(String donationId,
                                  String donationCode,
                                  String status,
                                  String hubName,
                                  String message,
                                  List<InboundParentCategory> registeredParentCategories) {
        this.donationId = safeText(donationId);
        this.donationCode = safeText(donationCode);
        this.status = safeText(status);
        this.hubName = safeText(hubName);
        this.message = safeText(message);
        this.registeredParentCategories = registeredParentCategories != null
                ? registeredParentCategories
                : new ArrayList<>();
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
    public String getStatus() {
        return status;
    }

    @NonNull
    public String getHubName() {
        return hubName;
    }

    @NonNull
    public String getMessage() {
        return message;
    }

    @NonNull
    public List<InboundParentCategory> getRegisteredParentCategories() {
        return registeredParentCategories;
    }

    private String safeText(String value) {
        return value == null ? "" : value.trim();
    }
}
