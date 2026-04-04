package com.drc.aidbridge.modules.volunteer.internal.entity;

import com.drc.aidbridge.modules.shared.enums.VehicleType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.locationtech.jts.geom.Point;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "volunteer_profiles")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Volunteer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(name = "is_online", nullable = false)
    @Builder.Default
    private boolean isOnline = false;

    @Column(name = "current_location", columnDefinition = "GEOGRAPHY(POINT,4326)")
    private Point currentLocation;
    


    @Enumerated(EnumType.STRING)
    @Column(name = "vehicle_type")
    private VehicleType vehicleType;

    @Column(name = "total_tasks_completed", nullable = false)
    @Builder.Default
    private Integer totalTasksCompleted = 0;

    @Column(name = "avg_rating", precision = 3, scale = 2)
    @Builder.Default
    private BigDecimal avgRating = BigDecimal.ZERO;

    @Column(name = "avg_response_seconds", nullable = false)
    @Builder.Default
    private Integer avgResponseSeconds = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
