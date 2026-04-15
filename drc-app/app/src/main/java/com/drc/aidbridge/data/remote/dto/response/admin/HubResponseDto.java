package com.drc.aidbridge.data.remote.dto.response.admin;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class HubResponseDto {

    @SerializedName("id")
    private String id;

    @SerializedName("name")
    private String name;

    @SerializedName("address")
    private String address;

    @SerializedName(value = "phone_number", alternate = { "phoneNumber" })
    private String phoneNumber;

    @SerializedName(value = "image_url", alternate = { "imageUrl" })
    private String imageUrl;

    @SerializedName(value = "operating_hours", alternate = { "operatingHours" })
    private String operatingHours;

    @SerializedName("status")
    private String status;

    @SerializedName(value = "created_at", alternate = { "createdAt" })
    private String createdAt;

    @SerializedName(value = "updated_at", alternate = { "updatedAt" })
    private String updatedAt;

    @SerializedName("location")
    private LocationDto location;

    @SerializedName(value = "inventory_groups", alternate = { "inventoryGroups" })
    private List<InventoryGroupDto> inventoryGroups;

    public String getId() {
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

    public String getCreatedAt() {
        return createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public LocationDto getLocation() {
        return location;
    }

    public List<InventoryGroupDto> getInventoryGroups() {
        return inventoryGroups;
    }

    public static class LocationDto {
        @SerializedName("lat")
        private Double lat;

        @SerializedName("lng")
        private Double lng;

        public Double getLat() {
            return lat;
        }

        public Double getLng() {
            return lng;
        }
    }

    public static class InventoryGroupDto {
        @SerializedName(value = "parent_category_name", alternate = { "parentCategoryName" })
        private String parentCategoryName;

        @SerializedName("items")
        private List<InventoryItemDto> items;

        public String getParentCategoryName() {
            return parentCategoryName;
        }

        public List<InventoryItemDto> getItems() {
            return items;
        }
    }

    public static class InventoryItemDto {
        @SerializedName(value = "item_category_id", alternate = { "itemCategoryId" })
        private String itemCategoryId;

        @SerializedName(value = "item_category_name", alternate = { "itemCategoryName" })
        private String itemCategoryName;

        @SerializedName("unit")
        private String unit;

        @SerializedName(value = "current_quantity", alternate = { "currentQuantity" })
        private Integer currentQuantity;

        @SerializedName(value = "low_stock_threshold", alternate = { "lowStockThreshold" })
        private Integer lowStockThreshold;

        @SerializedName(value = "last_restocked_at", alternate = { "lastRestockedAt" })
        private String lastRestockedAt;

        public String getItemCategoryId() {
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
