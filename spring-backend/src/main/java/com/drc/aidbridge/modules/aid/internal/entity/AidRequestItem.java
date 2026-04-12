package com.drc.aidbridge.modules.aid.internal.entity;

import jakarta.persistence.*;
import lombok.*;

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

    @Transient
    private Integer quantity;

    @Transient
    private String description;
}
