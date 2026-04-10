package com.drc.aidbridge.domain.model.victim;

import com.drc.aidbridge.data.remote.dto.response.victim.VictimHistoryDto;

import java.util.Collections;
import java.util.List;

public class VictimHistoryPage {

    private final List<VictimHistoryDto> items;
    private final boolean hasNextPage;
    private final boolean offlineData;

    public VictimHistoryPage(List<VictimHistoryDto> items, boolean hasNextPage, boolean offlineData) {
        this.items = items != null ? items : Collections.emptyList();
        this.hasNextPage = hasNextPage;
        this.offlineData = offlineData;
    }

    public List<VictimHistoryDto> getItems() {
        return items;
    }

    public boolean hasNextPage() {
        return hasNextPage;
    }

    public boolean isOfflineData() {
        return offlineData;
    }
}
