package com.drc.aidbridge.domain.model.victim;

public class VictimSupplyItem {

    private final String id;
    private final String name;

    public VictimSupplyItem(String id, String name) {
        this.id = id != null ? id.trim() : "";
        this.name = name != null ? name.trim() : "";
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
