package com.drc.aidbridge.data.remote.dto.response.staff;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class InboundDonationPreviewResponseDto {

    @SerializedName("type")
    private String type;

    @SerializedName("donationId")
    private String donationId;

    @SerializedName("donationCode")
    private String donationCode;

    @SerializedName("status")
    private String status;

    @SerializedName("hub")
    private HubDto hub;

    @SerializedName("registeredParentCategories")
    private List<ParentCategoryDto> registeredParentCategories;

    @SerializedName("message")
    private String message;

    public String getType() {
        return type;
    }

    public String getDonationId() {
        return donationId;
    }

    public String getDonationCode() {
        return donationCode;
    }

    public String getStatus() {
        return status;
    }

    public HubDto getHub() {
        return hub;
    }

    public List<ParentCategoryDto> getRegisteredParentCategories() {
        return registeredParentCategories;
    }

    public String getMessage() {
        return message;
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

    public static class ParentCategoryDto {
        @SerializedName("parentCategoryId")
        private String parentCategoryId;

        @SerializedName("parentCategoryName")
        private String parentCategoryName;

        @SerializedName("unit")
        private String unit;

        @SerializedName("availableSubCategories")
        private List<InboundSubCategoryDto> availableSubCategories;

        public String getParentCategoryId() {
            return parentCategoryId;
        }

        public String getParentCategoryName() {
            return parentCategoryName;
        }

        public String getUnit() {
            return unit;
        }

        public List<InboundSubCategoryDto> getAvailableSubCategories() {
            return availableSubCategories;
        }
    }
}
