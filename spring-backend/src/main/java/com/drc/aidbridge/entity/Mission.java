package com.drc.aidbridge.entity;

import com.drc.aidbridge.entity.enums.MissionStatus;
import com.drc.aidbridge.entity.enums.MissionType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "missions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Mission {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "mission_type", nullable = false)
    private MissionType missionType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sos_request_id")
    private SosRequest sosRequest;

    @Column(name = "aid_request_id")
    private UUID aidRequestId;

    @Column(name = "help_request_id")
    private UUID helpRequestId;

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

    @Column(name = "priority_score", precision = 5, scale = 2)
    private BigDecimal priorityScore;

    @Column(name = "victim_lat", precision = 9, scale = 6)
    private BigDecimal victimLat;

    @Column(name = "victim_lng", precision = 9, scale = 6)
    private BigDecimal victimLng;

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
}
