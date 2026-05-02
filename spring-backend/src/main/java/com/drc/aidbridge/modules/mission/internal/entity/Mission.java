package com.drc.aidbridge.modules.mission.internal.entity;

import com.drc.aidbridge.modules.shared.enums.MissionStatus;
import com.drc.aidbridge.modules.shared.enums.MissionType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "missions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Mission {
    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "mission_type", nullable = false)
    private MissionType missionType;

    @Column(name = "sos_request_id")
    private UUID sosRequestId;

    @Column(name = "aid_request_id")
    private UUID aidRequestId;

    @Column(name = "volunteer_id")
    private UUID volunteerId;

    @Column(name = "hub_id")
    private UUID hubId;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    @Builder.Default
    private MissionStatus status = MissionStatus.PENDING;

    @Column(name = "qr_code_token", length = 100, unique = true)
    private String qrCodeToken;

    @Column(name = "code_name", length = 20, nullable = false)
    private String codeName;

    @Column(name = "priority_score", precision = 5, scale = 2)
    private BigDecimal priorityScore;

    @Column(name = "victim_location", columnDefinition = "geography(Point,4326)")
    private Point victimLocation;

    @Column(name = "accepted_at")
    private Instant acceptedAt;

    @Column(name = "picked_up_at")
    private Instant pickedUpAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "cancellation_reason", columnDefinition = "text")
    private String cancellationReason;

    @Column(name = "confirmation_image_url", length = 500)
    private String confirmationImageUrl;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "comment", columnDefinition = "text")
    private String comment;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public static Point createPoint(BigDecimal lat, BigDecimal lng) {
        if (lat == null || lng == null) {
            return null;
        }
        Point point = GEOMETRY_FACTORY.createPoint(new Coordinate(lng.doubleValue(), lat.doubleValue()));
        point.setSRID(4326);
        return point;
    }

    @Transient
    public BigDecimal getVictimLat() {
        if (victimLocation == null)
            return null;
        return BigDecimal.valueOf(victimLocation.getY());
    }

    @Transient
    public BigDecimal getVictimLng() {
        if (victimLocation == null)
            return null;
        return BigDecimal.valueOf(victimLocation.getX());
    }
}
