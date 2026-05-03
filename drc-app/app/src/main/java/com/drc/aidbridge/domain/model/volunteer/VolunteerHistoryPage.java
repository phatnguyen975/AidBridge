package com.drc.aidbridge.domain.model.volunteer;

import java.util.Collections;
import java.util.List;

public class VolunteerHistoryPage {

    private final List<VolunteerHistoryItem> items;
    private final int page;
    private final int limit;
    private final long total;
    private final int totalPages;
    private final boolean hasNext;
    private final boolean hasPrevious;

    public VolunteerHistoryPage(List<VolunteerHistoryItem> items,
            int page,
            int limit,
            long total,
            int totalPages,
            boolean hasNext,
            boolean hasPrevious) {
        this.items = items != null ? items : Collections.emptyList();
        this.page = page;
        this.limit = limit;
        this.total = total;
        this.totalPages = totalPages;
        this.hasNext = hasNext;
        this.hasPrevious = hasPrevious;
    }

    public List<VolunteerHistoryItem> getItems() {
        return items;
    }

    public int getPage() {
        return page;
    }

    public int getLimit() {
        return limit;
    }

    public long getTotal() {
        return total;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public boolean isHasNext() {
        return hasNext;
    }

    public boolean isHasPrevious() {
        return hasPrevious;
    }
}