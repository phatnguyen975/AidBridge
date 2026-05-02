package com.drc.aidbridge.modules.routing.internal.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.PrecisionModel;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "dangerous_zones")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DangerousZone {
    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "name", nullable = false, length = 200)
    private String name;


    @Column(name = "area", columnDefinition = "geography(Polygon,4326)")
    private Polygon area;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    
    @Column(name = "admin_id")
    private UUID adminId;

    public static Polygon createPolygon(org.locationtech.jts.geom.Coordinate[] coordinates) {
        if (coordinates == null || coordinates.length < 4) {
            throw new IllegalArgumentException("Polygon must have at least 4 coordinates (including closed loop)");
        }
        org.locationtech.jts.geom.LinearRing shell = GEOMETRY_FACTORY.createLinearRing(coordinates);
        Polygon polygon = GEOMETRY_FACTORY.createPolygon(shell, null);
        polygon.setSRID(4326);
        return polygon;
    }
}
