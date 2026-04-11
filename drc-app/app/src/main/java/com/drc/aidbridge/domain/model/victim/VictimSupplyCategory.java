package com.drc.aidbridge.domain.model.victim;

import java.util.Collections;
import java.util.List;

public class VictimSupplyCategory {

    private final String id;
    private final String name;
    private final List<VictimSupplyItem> items;

    public VictimSupplyCategory(String id, String name, List<VictimSupplyItem> items) {
        this.id = id != null ? id.trim() : "";
        this.name = name != null ? name.trim() : "";
        this.items = items != null ? items : Collections.emptyList();
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<VictimSupplyItem> getItems() {
        return items;
    }
}
