package com.drc.aidbridge.modules.donation.internal.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "donation_items")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DonationItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "donation_id", nullable = false)
    private UUID donationId;

    @Column(name = "item_category_id", nullable = false)
    private UUID itemCategoryId;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "unit", length = 100)
    private String unit;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
