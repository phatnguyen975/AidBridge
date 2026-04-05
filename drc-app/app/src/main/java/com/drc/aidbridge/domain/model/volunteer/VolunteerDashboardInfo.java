package com.drc.aidbridge.domain.model.volunteer;

public class VolunteerDashboardInfo {

    private final String fullName;
    private final boolean isOnline;
    private final int totalCompletedTasks;

    public VolunteerDashboardInfo(String fullName, boolean isOnline, int totalCompletedTasks) {
        this.fullName = fullName;
        this.isOnline = isOnline;
        this.totalCompletedTasks = totalCompletedTasks;
    }

    public String getFullName() {
        return fullName;
    }

    public boolean isOnline() {
        return isOnline;
    }

    public int getTotalCompletedTasks() {
        return totalCompletedTasks;
    }
}
