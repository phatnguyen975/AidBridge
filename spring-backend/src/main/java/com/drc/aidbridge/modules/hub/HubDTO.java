package com.drc.aidbridge.modules.hub;

import com.drc.aidbridge.modules.shared.enums.HubStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
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
    private List<InventoryItemDTO> inventory;

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
        private Integer currentQuantity;
        private Integer lowStockThreshold;
        private Instant lastRestockedAt;
    }
}
