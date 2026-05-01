package com.drc.aidbridge.data.remote.dto.response.staff;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class StaffInventoryResponseDto {

    @SerializedName("hub")
    private HubDto hub;

    @SerializedName("filters")
    private List<FilterDto> filters;

    @SerializedName("items")
    private List<ItemDto> items;

    @SerializedName("totalItems")
    private Long totalItems;

    public HubDto getHub() {
        return hub;
    }

    public List<FilterDto> getFilters() {
        return filters;
    }

    public List<ItemDto> getItems() {
        return items;
    }

    public Long getTotalItems() {
        return totalItems;
    }

    public static class HubDto {
        @SerializedName("id")
        private String id;

        @SerializedName("name")
        private String name;

        @SerializedName("address")
        private String address;

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getAddress() {
            return address;
        }
    }

    public static class FilterDto {
        @SerializedName("id")
        private String id;

        @SerializedName("name")
        private String name;

        @SerializedName("type")
        private String type;

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getType() {
            return type;
        }
    }

    public static class ItemDto {
        @SerializedName("inventoryId")
        private String inventoryId;

        @SerializedName("itemCategoryId")
        private String itemCategoryId;

        @SerializedName("name")
        private String name;

        @SerializedName("unit")
        private String unit;

        @SerializedName("iconUrl")
        private String iconUrl;

        @SerializedName("parentCategoryId")
        private String parentCategoryId;

        @SerializedName("parentCategoryName")
        private String parentCategoryName;

        @SerializedName("currentQuantity")
        private Integer currentQuantity;

        @SerializedName("lowStockThreshold")
        private Integer lowStockThreshold;

        @SerializedName("isLowStock")
        private Boolean lowStock;

        @SerializedName("lastRestockedAt")
        private String lastRestockedAt;

        public String getInventoryId() {
            return inventoryId;
        }

        public String getItemCategoryId() {
            return itemCategoryId;
        }

        public String getName() {
            return name;
        }

        public String getUnit() {
            return unit;
        }

        public String getIconUrl() {
            return iconUrl;
        }

        public String getParentCategoryId() {
            return parentCategoryId;
        }

        public String getParentCategoryName() {
            return parentCategoryName;
        }

        public Integer getCurrentQuantity() {
            return currentQuantity;
        }

        public Integer getLowStockThreshold() {
            return lowStockThreshold;
        }

        public Boolean isLowStock() {
            return lowStock;
        }

        public String getLastRestockedAt() {
            return lastRestockedAt;
        }
    }
}
