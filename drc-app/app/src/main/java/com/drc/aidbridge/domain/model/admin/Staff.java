package com.drc.aidbridge.domain.model.admin;

public class Staff {

    private final String id;
    private final String userId;
    private final String fullName;
    private final String email;
    private final String phoneNumber;
    private final String hubId;
    private final String hubName;

    public Staff(String id,
                 String userId,
                 String fullName,
                 String email,
                 String phoneNumber,
                 String hubId,
                 String hubName) {
        this.id = safeText(id);
        this.userId = safeText(userId);
        this.fullName = safeText(fullName);
        this.email = safeText(email);
        this.phoneNumber = safeText(phoneNumber);
        this.hubId = safeText(hubId);
        this.hubName = safeText(hubName);
    }

    public String getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getHubId() {
        return hubId;
    }

    public String getHubName() {
        return hubName;
    }

    private String safeText(String value) {
        return value == null ? "" : value.trim();
    }
}
