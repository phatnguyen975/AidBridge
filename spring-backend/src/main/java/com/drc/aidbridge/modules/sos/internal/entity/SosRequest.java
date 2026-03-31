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
    @Column(name = "requester_id")
    private UUID requesterId;
    @Column(name = "lat", nullable = false)
    private Double lat;
    @Column(name = "lng", nullable = false)
    private Double lng;
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
}
