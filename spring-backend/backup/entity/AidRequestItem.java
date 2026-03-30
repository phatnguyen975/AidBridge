package com.drc.aidbridge.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "aid_request_items")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AidRequestItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "aid_request_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private AidRequest aidRequest;

    @Column(name = "item_category_id", nullable = false)
    private UUID itemCategoryId;

    @Column(nullable = false)
    private Integer quantity;

    @Column(columnDefinition = "text")
    private String description;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
