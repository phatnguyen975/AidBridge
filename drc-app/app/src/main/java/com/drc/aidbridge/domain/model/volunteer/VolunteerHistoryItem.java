package com.drc.aidbridge.domain.model.volunteer;

public class VolunteerHistoryItem {

    private final String missionId;
    private final String type;
    private final String completedAt;
    private final String location;

    public VolunteerHistoryItem(String missionId, String type, String completedAt, String location) {
        this.missionId = missionId;
        this.type = type;
        this.completedAt = completedAt;
        this.location = location;
    }

    public String getMissionId() {
        return missionId;
    }

    public String getType() {
        return type;
    }

    public String getCompletedAt() {
        return completedAt;
    }

    public String getLocation() {
        return location;
    }
}
