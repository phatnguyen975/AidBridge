package com.drc.aidbridge.domain.model.admin;

import com.drc.aidbridge.domain.enums.HubStatus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class Hub {

    private final UUID id;
    private final String name;
    private final String address;
    private final String phoneNumber;
    private final String imageUrl;
    private final String status;
    private final String operatingHours;
    private final String createdAt;
    private final String updatedAt;
    private final Location location;
    private final List<InventoryGroup> inventoryGroups;
    private final long totalImportedQuantity;
    private final long totalExportedQuantity;

    public Hub(UUID id,
            String name,
            String address,
            String imageUrl,
            String operatingHours,
            String status) {
        this(id, name, address, "", imageUrl, status, operatingHours, "", "", null, Collections.emptyList(), 0L, 0L);
    }

    public Hub(UUID id,
            String name,
            String address,
            String phoneNumber,
            String imageUrl,
            String status,
            String operatingHours,
            String createdAt,
            String updatedAt,
            Location location,
            List<InventoryGroup> inventoryGroups,
            long totalImportedQuantity,
            long totalExportedQuantity) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.phoneNumber = phoneNumber;
        this.imageUrl = imageUrl;
        this.status = status;
        this.operatingHours = operatingHours;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.location = location;
        this.inventoryGroups = inventoryGroups == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(inventoryGroups));
        this.totalImportedQuantity = Math.max(totalImportedQuantity, 0L);
        this.totalExportedQuantity = Math.max(totalExportedQuantity, 0L);
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

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getOperatingHours() {
        return operatingHours;
    }

    public String getStatus() {
        return status;
    }

    public HubStatus getStatusEnum() {
        return HubStatus.fromStringSafe(status);
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public Location getLocation() {
        return location;
    }

    public List<InventoryGroup> getInventoryGroups() {
        return inventoryGroups;
    }

    public boolean isActive() {
        return getStatusEnum() == HubStatus.ACTIVE;
    }

    public long getTotalImportedQuantity() {
        return totalImportedQuantity;
    }

    public long getTotalExportedQuantity() {
        return totalExportedQuantity;
    }

    public static class Location {
        private final Double lat;
        private final Double lng;

        public Location(Double lat, Double lng) {
            this.lat = lat;
            this.lng = lng;
        }

        public Double getLat() {
            return lat;
        }

        public Double getLng() {
            return lng;
        }
    }

    public static class InventoryGroup {
        private final String parentCategoryName;
        private final List<InventoryItem> items;

        public InventoryGroup(String parentCategoryName, List<InventoryItem> items) {
            this.parentCategoryName = parentCategoryName;
            this.items = items == null
                    ? Collections.emptyList()
                    : Collections.unmodifiableList(new ArrayList<>(items));
        }

        public String getParentCategoryName() {
            return parentCategoryName;
        }

        public List<InventoryItem> getItems() {
            return items;
        }
    }

    public static class InventoryItem {
        private final UUID itemCategoryId;
        private final String itemCategoryName;
        private final String unit;
        private final Integer currentQuantity;
        private final Integer lowStockThreshold;
        private final String lastRestockedAt;

        public InventoryItem(UUID itemCategoryId,
                String itemCategoryName,
                String unit,
                Integer currentQuantity,
                Integer lowStockThreshold,
                String lastRestockedAt) {
            this.itemCategoryId = itemCategoryId;
            this.itemCategoryName = itemCategoryName;
            this.unit = unit;
            this.currentQuantity = currentQuantity;
            this.lowStockThreshold = lowStockThreshold;
            this.lastRestockedAt = lastRestockedAt;
        }

        public UUID getItemCategoryId() {
            return itemCategoryId;
        }

        public String getItemCategoryName() {
            return itemCategoryName;
        }

        public String getUnit() {
            return unit;
        }

        public Integer getCurrentQuantity() {
            return currentQuantity;
        }

        public Integer getLowStockThreshold() {
            return lowStockThreshold;
        }

        public String getLastRestockedAt() {
            return lastRestockedAt;
        }
    }
}
