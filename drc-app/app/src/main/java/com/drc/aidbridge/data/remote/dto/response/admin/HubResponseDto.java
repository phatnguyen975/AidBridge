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

    @SerializedName("latitude")
    private Double latitude;

    @SerializedName(value = "longitude", alternate = { "lng" })
    private Double longitude;

    @SerializedName(value = "inventory_groups", alternate = { "inventoryGroups" })
    private List<InventoryGroupDto> inventoryGroups;

    @SerializedName(value = "total_imported_quantity", alternate = { "totalImportedQuantity" })
    private Long totalImportedQuantity;

    @SerializedName(value = "total_exported_quantity", alternate = { "totalExportedQuantity" })
    private Long totalExportedQuantity;

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

    public Double getLatitude() {
        return latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public List<InventoryGroupDto> getInventoryGroups() {
        return inventoryGroups;
    }

    public Long getTotalImportedQuantity() {
        return totalImportedQuantity;
    }

    public Long getTotalExportedQuantity() {
        return totalExportedQuantity;
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

        @SerializedName("category")
        private String category;

        @SerializedName("items")
        private List<InventoryItemDto> items;

        public String getParentCategoryName() {
            return parentCategoryName != null ? parentCategoryName : category;
        }

        public List<InventoryItemDto> getItems() {
            return items;
        }
    }

    public static class InventoryItemDto {
        @SerializedName(value = "item_category_id", alternate = { "itemCategoryId" })
        private String itemCategoryId;

        @SerializedName(value = "item_id", alternate = { "itemId" })
        private String itemId;

        @SerializedName(value = "item_category_name", alternate = { "itemCategoryName" })
        private String itemCategoryName;

        @SerializedName("name")
        private String name;

        @SerializedName("unit")
        private String unit;

        @SerializedName(value = "current_quantity", alternate = { "currentQuantity" })
        private Integer currentQuantity;

        @SerializedName("quantity")
        private Integer quantity;

        @SerializedName(value = "low_stock_threshold", alternate = { "lowStockThreshold" })
        private Integer lowStockThreshold;

        @SerializedName(value = "last_restocked_at", alternate = { "lastRestockedAt" })
        private String lastRestockedAt;

        public String getItemCategoryId() {
            return itemCategoryId != null ? itemCategoryId : itemId;
        }

        public String getItemCategoryName() {
            return itemCategoryName != null ? itemCategoryName : name;
        }

        public String getUnit() {
            return unit;
        }

        public Integer getCurrentQuantity() {
            return currentQuantity != null ? currentQuantity : quantity;
        }

        public Integer getLowStockThreshold() {
            return lowStockThreshold;
        }

        public String getLastRestockedAt() {
            return lastRestockedAt;
        }
    }
}
