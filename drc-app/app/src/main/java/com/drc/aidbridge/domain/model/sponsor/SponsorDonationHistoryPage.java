package com.drc.aidbridge.domain.model.sponsor;

import java.util.Collections;
import java.util.List;

public class SponsorDonationHistoryPage {

    private final List<SponsorDonationHistoryItem> items;
    private final int page;
    private final int limit;
    private final long total;
    private final int totalPages;
    private final boolean hasNext;

    public SponsorDonationHistoryPage(List<SponsorDonationHistoryItem> items,
                                      int page,
                                      int limit,
                                      long total,
                                      int totalPages,
                                      boolean hasNext) {
        this.items = items != null ? items : Collections.emptyList();
        this.page = page;
        this.limit = limit;
        this.total = total;
        this.totalPages = totalPages;
        this.hasNext = hasNext;
    }

    public List<SponsorDonationHistoryItem> getItems() {
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
}
