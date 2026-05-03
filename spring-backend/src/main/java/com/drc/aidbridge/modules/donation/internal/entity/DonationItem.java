package com.drc.aidbridge.modules.donation.internal.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
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

    @Column(name = "item_category_id")
    private UUID itemCategoryId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
