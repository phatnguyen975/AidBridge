package com.drc.aidbridge.domain.model.admin;

public class HubSummary {

    private final int totalHubs;
    private final int activeHubs;
    private final int inactiveHubs;

    public HubSummary(int totalHubs, int activeHubs, int inactiveHubs) {
        this.totalHubs = Math.max(totalHubs, 0);
        this.activeHubs = Math.max(activeHubs, 0);
        this.inactiveHubs = Math.max(inactiveHubs, 0);
    }

    public int getTotalHubs() {
        return totalHubs;
    }

    public int getActiveHubs() {
        return activeHubs;
    }

    public int getInactiveHubs() {
        return inactiveHubs;
    }
}
