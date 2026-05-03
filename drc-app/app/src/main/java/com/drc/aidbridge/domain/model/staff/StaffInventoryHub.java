package com.drc.aidbridge.domain.model.staff;

import androidx.annotation.NonNull;

public class StaffInventoryHub {

    private final String id;
    private final String name;
    private final String address;

    public StaffInventoryHub(String id, String name, String address) {
        this.id = safeText(id);
        this.name = safeText(name);
        this.address = safeText(address);
    }

    @NonNull
    public String getId() {
        return id;
    }

    @NonNull
    public String getName() {
        return name;
    }

    @NonNull
    public String getAddress() {
        return address;
    }

    private String safeText(String value) {
        return value == null ? "" : value.trim();
    }
}
