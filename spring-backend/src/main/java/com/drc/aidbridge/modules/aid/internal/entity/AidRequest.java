package com.drc.aidbridge.modules.aid.internal.entity;

import com.drc.aidbridge.modules.shared.enums.AidStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;

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

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "requester_id", nullable = false)
    private UUID requesterId;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private AidStatus status = AidStatus.PENDING;

    @Column(name = "location", nullable = false, columnDefinition = "geography(POINT, 4326)")
    private Point location;

    @Column(name = "address", length = 500)
    private String address;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "number_elderly", nullable = false)
    @Builder.Default
    @Min(0)
    private Integer numberElderly = 0;

    @Column(name = "number_adult", nullable = false)
    @Builder.Default
    @Min(0)
    private Integer numberAdult = 0;

    @Column(name = "number_children", nullable = false)
    @Builder.Default
    @Min(0)
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

    /**
     * Creates a Point from latitude and longitude coordinates.
     * Uses SRID 4326 (WGS84) for geographic coordinate system.
     *
     * @param lat latitude coordinate
     * @param lng longitude coordinate
     * @return Point representing the location
     */
    public static Point createPoint(Double lat, Double lng) {
        if (lat == null || lng == null) {
            return null;
        }
        Point point = GEOMETRY_FACTORY.createPoint(new Coordinate(lng, lat));
        point.setSRID(4326);
        return point;
    }

    /**
     * Gets the latitude from the location Point.
     * 
     * @return latitude as BigDecimal, or null if location is null
     */
    @Transient
    public java.math.BigDecimal getLat() {
        return location != null ? java.math.BigDecimal.valueOf(location.getY()) : null;
    }

    /**
     * Gets the longitude from the location Point.
     * 
     * @return longitude as BigDecimal, or null if location is null
     */
    @Transient
    public java.math.BigDecimal getLng() {
        return location != null ? java.math.BigDecimal.valueOf(location.getX()) : null;
    }
}
