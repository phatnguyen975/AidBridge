package com.drc.aidbridge.modules.hub;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.drc.aidbridge.modules.shared.enums.HubStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HubDTO {
    private UUID id;
    private String name;
    private String address;
    private String phoneNumber;
    private String imageUrl;
    private HubStatus status;
    private String operatingHours;
    private Instant createdAt;
    private Instant updatedAt;
    private LocationDTO location;

    // Always expose grouped inventory in JSON (at minimum as an empty array).
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @Builder.Default
    private List<ParentCategoryInventoryDTO> inventoryGroups = new ArrayList<>();

    public List<ParentCategoryInventoryDTO> getInventoryGroups() {
        if (inventoryGroups == null) {
            inventoryGroups = new ArrayList<>();
        }
        return inventoryGroups;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LocationDTO {
        private BigDecimal lat;
        private BigDecimal lng;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InventoryItemDTO {
        private UUID itemCategoryId;
        private String itemCategoryName;
        private String unit;
        private Integer currentQuantity;
        private Integer lowStockThreshold;
        private Instant lastRestockedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParentCategoryInventoryDTO {
        private String parentCategoryName;

        @Builder.Default
        private List<InventoryItemDTO> items = new ArrayList<>();
    }
}
