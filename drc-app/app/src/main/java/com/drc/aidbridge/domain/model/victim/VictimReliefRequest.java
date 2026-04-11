package com.drc.aidbridge.domain.model.victim;

import java.util.Collections;
import java.util.List;

public class VictimReliefRequest {

    private final int adultsCount;
    private final int eldersCount;
    private final int childrenCount;
    private final String note;
    private final List<VictimRequestedItem> requestedItems;

    public VictimReliefRequest(int adultsCount,
                               int eldersCount,
                               int childrenCount,
                               String note,
                               List<VictimRequestedItem> requestedItems) {
        this.adultsCount = Math.max(0, adultsCount);
        this.eldersCount = Math.max(0, eldersCount);
        this.childrenCount = Math.max(0, childrenCount);
        this.note = note != null ? note.trim() : "";
        this.requestedItems = requestedItems != null ? requestedItems : Collections.emptyList();
    }

    public int getAdultsCount() {
        return adultsCount;
    }

    public int getEldersCount() {
        return eldersCount;
    }

    public int getChildrenCount() {
        return childrenCount;
    }

    public String getNote() {
        return note;
    }

    public List<VictimRequestedItem> getRequestedItems() {
        return requestedItems;
    }
}
