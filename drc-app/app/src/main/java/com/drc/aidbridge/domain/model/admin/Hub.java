package com.drc.aidbridge.domain.model.admin;

import com.drc.aidbridge.domain.enums.HubStatus;

import java.util.UUID;

public class Hub {

    private final UUID id;
    private final String name;
    private final String address;
    private final HubStatus status;

    public Hub(UUID id, String name, String address, HubStatus status) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.status = status;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }

    public HubStatus getStatus() {
        return status;
    }

    public boolean isActive() {
        return status == HubStatus.ACTIVE;
    }
}
