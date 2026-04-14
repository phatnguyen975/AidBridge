package com.drc.aidbridge.modules.hub.internal.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "hub_inventories")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HubInventory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "hub_id", nullable = false)
    private UUID hubId;

    @Column(name = "item_category_id", nullable = false)
    private UUID itemCategoryId;

    @Column(name = "current_quantity", nullable = false)
    private Integer currentQuantity;

    @Column(name = "low_stock_threshold", nullable = false)
    private Integer lowStockThreshold;

    @Column(name = "last_restocked_at")
    private Instant lastRestockedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
