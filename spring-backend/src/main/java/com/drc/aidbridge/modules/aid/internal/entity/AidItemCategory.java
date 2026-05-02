package com.drc.aidbridge.modules.aid.internal.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "item_categories")
@Getter
@Setter
@NoArgsConstructor
public class AidItemCategory {

    @Id
    private UUID id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "parent_id")
    private UUID parentId;

    @Column(name = "unit", nullable = false, length = 50)
    private String unit;

    @Column(name = "icon_url")
    private String iconUrl;

    @Column(name = "is_leaf", nullable = false)
    private boolean isLeaf;

    @Column(name = "sort_order")
    private Integer sortOrder;
}
