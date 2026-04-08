package com.drc.aidbridge.modules.hub.internal.entity;

import com.drc.aidbridge.modules.shared.enums.HubStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.locationtech.jts.geom.Point;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "hubs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Hub {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "name", length = 255)
    private String name;

    @Column(name = "address", length = 500)
    private String address;

    @Column(name = "phone_number", length = 50)
    private String phoneNumber;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private HubStatus status = HubStatus.ACTIVE;

    @Column(name = "operating_hours", length = 255)
    private String operatingHours;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "location", columnDefinition = "geography(Point,4326)")
    private Point location;

    @Transient
    public BigDecimal getLat() {
        if (location == null) return null;
        return BigDecimal.valueOf(location.getY());
    }

    @Transient
    public BigDecimal getLng() {
        if (location == null) return null;
        return BigDecimal.valueOf(location.getX());
    }
}
