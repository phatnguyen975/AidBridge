package com.drc.aidbridge.domain.model.volunteer;

public class VolunteerPersonalInfo {

    private final String fullName;
    private final String phoneNumber;
    private final String email;

    public VolunteerPersonalInfo(String fullName, String phoneNumber, String email) {
        this.fullName = fullName;
        this.phoneNumber = phoneNumber;
        this.email = email;
    }

    public String getFullName() {
        return fullName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getEmail() {
        return email;
    }
}
