package com.drc.aidbridge.domain.model.admin;

import com.drc.aidbridge.domain.enums.HubStatus;

import java.util.UUID;

public class Hub {

    private final UUID id;
    private final String name;
    private final String address;
    private final String imageUrl;
    private final String operatingHours;
    private final HubStatus status;

    public Hub(UUID id, String name, String address, String imageUrl, String operatingHours, HubStatus status) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.imageUrl = imageUrl;
        this.operatingHours = operatingHours;
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

    public String getImageUrl() {
        return imageUrl;
    }

    public String getOperatingHours() {
        return operatingHours;
    }

    public HubStatus getStatus() {
        return status;
    }

    public boolean isActive() {
        return status == HubStatus.ACTIVE;
    }
}
