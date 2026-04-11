package com.drc.aidbridge.domain.model.victim;

import java.util.Collections;
import java.util.List;

public class VictimHistoryPage {

    private final List<VictimHistoryItem> items;
    private final boolean hasNextPage;
    private final boolean offlineData;

    public VictimHistoryPage(List<VictimHistoryItem> items, boolean hasNextPage, boolean offlineData) {
        this.items = items != null ? items : Collections.emptyList();
        this.hasNextPage = hasNextPage;
        this.offlineData = offlineData;
    }

    public List<VictimHistoryItem> getItems() {
        return items;
    }

    public boolean hasNextPage() {
        return hasNextPage;
    }

    public boolean isOfflineData() {
        return offlineData;
    }
}
