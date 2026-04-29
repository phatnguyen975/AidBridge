package com.drc.aidbridge.modules.sos.internal.entity;

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
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "urgency_level", nullable = false)
    private UrgencyLevel urgencyLevel;
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status", nullable = false)
    private SosStatus status;
    @Column(name = "image_url", length = 500)
    private String imageUrl;
    @Column(name = "client_request_id", length = 100, unique = true)
    private String clientRequestId;
    @Column(name = "source", length = 30)
    private String source;
    @Column(name = "quick_sos")
    private Boolean quickSos;
    @Column(name = "accuracy")
    private Double accuracy;
    @Column(name = "triggered_at")
    private Instant triggeredAt;
    @Column(name = "location_captured_at")
    private Instant locationCapturedAt;
    @Column(name = "device_info", length = 500)
    private String deviceInfo;
    @Column(name = "sender_phone", length = 50)
    private String senderPhone;
    @Column(name = "raw_message", columnDefinition = "text")
    private String rawMessage;
    @Column(name = "received_at_gateway_millis")
    private Long receivedAtGatewayMillis;
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
