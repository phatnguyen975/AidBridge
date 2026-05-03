package com.drc.aidbridge.domain.model.staff;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

public class OutboundAidRequestDetail {

    private final String id;
    private final String description;
    private final int numberAdult;
    private final int numberElderly;
    private final int numberChildren;
    private final List<OutboundAidRequestItem> items;

    public OutboundAidRequestDetail(String id,
                                    String description,
                                    int numberAdult,
                                    int numberElderly,
                                    int numberChildren,
                                    List<OutboundAidRequestItem> items) {
        this.id = safeText(id);
        this.description = safeText(description);
        this.numberAdult = Math.max(numberAdult, 0);
        this.numberElderly = Math.max(numberElderly, 0);
        this.numberChildren = Math.max(numberChildren, 0);
        this.items = items != null ? items : new ArrayList<>();
    }

    @NonNull
    public String getId() {
        return id;
    }

    @NonNull
    public String getDescription() {
        return description;
    }

    public int getNumberAdult() {
        return numberAdult;
    }

    public int getNumberElderly() {
        return numberElderly;
    }

    public int getNumberChildren() {
        return numberChildren;
    }

    @NonNull
    public List<OutboundAidRequestItem> getItems() {
        return items;
    }

    private String safeText(String value) {
        return value == null ? "" : value.trim();
    }
}
