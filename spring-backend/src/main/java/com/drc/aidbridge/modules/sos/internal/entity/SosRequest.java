package com.drc.aidbridge.modules.sos.internal.entity;

// ... existing code ...
import com.drc.aidbridge.modules.shared.enums.SosStatus;
import com.drc.aidbridge.modules.shared.enums.UrgencyLevel;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import java.math.BigDecimal;

@Entity
@Table(name = "sos_requests")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SosRequest {
    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "requester_id")
    private UUID requesterId;
    @Column(name = "location", columnDefinition = "geography(Point,4326)")
    private Point location;
    @Column(name = "address", length = 500)
    private String address;
    @Column(columnDefinition = "text")
    private String description;
    @Column(name = "people_count", nullable = false)
    @Builder.Default
    private Integer peopleCount = 1;
    @Enumerated(EnumType.STRING)
    @Column(name = "urgency_level", nullable = false)
    @Builder.Default
    private UrgencyLevel urgencyLevel = UrgencyLevel.MEDIUM;
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status", nullable = false)
    private SosStatus status;
    @Column(name = "image_url", length = 500)
    private String imageUrl;
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public static Point createPoint(Double lat, Double lng) {
        if (lat == null || lng == null) {
            throw new IllegalArgumentException("Lat/Lng must not be null");
        }
        Point point = GEOMETRY_FACTORY.createPoint(new Coordinate(lng, lat));
        point.setSRID(4326);
        return point;
    }

    @Transient
    public BigDecimal getLat() {
        return location != null ? BigDecimal.valueOf(location.getY()) : null;
    }

    @Transient
    public BigDecimal getLng() {
        return location != null ? BigDecimal.valueOf(location.getX()) : null;
    }
}
