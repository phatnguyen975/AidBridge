package com.drc.aidbridge.modules.mission.internal.entity;

import com.drc.aidbridge.modules.shared.enums.DispatchResponse;
import com.drc.aidbridge.modules.shared.enums.DispatchType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Entity đại diện cho lịch sử điều phối volunteer cho mission.
 * Tương ứng với bảng dispatch_attempts trong database schema.
 */
@Entity
@Table(name = "dispatch_attempts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DispatchAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "mission_id", nullable = false)
    private UUID missionId;

    @Column(name = "volunteer_id", nullable = false)
    private UUID volunteerId;

    @Column(name = "dispatch_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private DispatchType dispatchType;

    @Column(name = "batch_number", nullable = false)
    @Builder.Default
    private Integer batchNumber = 1;

    @Column(name = "radius_km", precision = 5, scale = 2)
    private BigDecimal radiusKm;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private DispatchResponse response = DispatchResponse.PENDING;

    @Column(name = "responded_at")
    private Instant respondedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
