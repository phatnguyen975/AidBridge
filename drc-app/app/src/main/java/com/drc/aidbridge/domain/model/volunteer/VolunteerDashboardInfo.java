package com.drc.aidbridge.domain.model.volunteer;

public class VolunteerDashboardInfo {

    private final String fullName;
    private final boolean isOnline;
    private final int totalCompletedTasks;
    private String avatarUrl;

    public VolunteerDashboardInfo(String fullName, boolean isOnline, int totalCompletedTasks) {
        this(fullName, isOnline, totalCompletedTasks, null);
    }

    public VolunteerDashboardInfo(String fullName, boolean isOnline, int totalCompletedTasks, String avatarUrl) {
        this.fullName = fullName;
        this.isOnline = isOnline;
        this.totalCompletedTasks = totalCompletedTasks;
        this.avatarUrl = avatarUrl;
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

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }
}
