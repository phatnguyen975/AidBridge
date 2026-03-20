package com.drc.aidbridge.entity;

import com.drc.aidbridge.entity.enums.SosStatus;
import com.drc.aidbridge.entity.enums.UrgencyLevel;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "sos_requests")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SosRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "requester_id", nullable = false)
    private UUID requesterId;

    @Column(name = "requester_name", nullable = false, length = 100)
    private String requesterName;

    @Column(name = "requester_phone", nullable = false, length = 15)
    private String requesterPhone;

    @Column(name = "victim_name", length = 100)
    private String victimName;

    @Column(name = "victim_phone", length = 15)
    private String victimPhone;

   @Column(name = "victim_lat", nullable = false)
    private Double victimLat;

    @Column(name = "victim_lng", nullable = false)
    private Double victimLng;

    @Column(name = "victim_address", length = 255)
    private String victimAddress;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "people_count", nullable = false)
    @Builder.Default
    private Integer peopleCount = 1;

    @Column(name = "is_on_behalf", nullable = false)
    @Builder.Default
    private Boolean isOnBehalf = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "urgency_level", nullable = false)
    @Builder.Default
    private UrgencyLevel urgencyLevel = UrgencyLevel.MEDIUM;

    @Column(name = "ai_summary", columnDefinition = "text")
    private String aiSummary;

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
}
