package com.drc.aidbridge.modules.aid.internal.entity;

import com.drc.aidbridge.modules.shared.enums.AidStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "aid_requests")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AidRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "requester_id", nullable = false)
    private UUID requesterId;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private AidStatus status = AidStatus.PENDING;

    @Column(name = "lat", nullable = false, precision = 9, scale = 6)
    private BigDecimal lat;

    @Column(name = "lng", nullable = false, precision = 9, scale = 6)
    private BigDecimal lng;

    @Column(name = "address", length = 500)
    private String address;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "number_elderly", nullable = false)
    @Builder.Default
    private Integer numberElderly = 0;

    @Column(name = "number_adult", nullable = false)
    @Builder.Default
    private Integer numberAdult = 0;

    @Column(name = "number_children", nullable = false)
    @Builder.Default
    private Integer numberChildren = 0;

    @OneToMany(mappedBy = "aidRequest", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Builder.Default
    private List<AidRequestItem> items = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
