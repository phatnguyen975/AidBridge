package com.drc.aidbridge.modules.shelter.internal.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "shelters")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Shelter {

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "hub_id")
    private UUID hubId;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "address", length = 500)
    private String address;

    @Column(name = "max_capacity", nullable = false)
    private Integer maxCapacity;

    @Column(name = "current_capacity", nullable = false)
    private Integer currentCapacity;

    @Column(name = "phone_number", length = 50)
    private String phoneNumber;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "location", columnDefinition = "geography(Point,4326)")
    private Point location;

    public static Point createPoint(BigDecimal lat, BigDecimal lng) {
        if (lat == null || lng == null) return null;
        Point p = GEOMETRY_FACTORY.createPoint(new Coordinate(lng.doubleValue(), lat.doubleValue()));
        p.setSRID(4326);
        return p;
    }

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
