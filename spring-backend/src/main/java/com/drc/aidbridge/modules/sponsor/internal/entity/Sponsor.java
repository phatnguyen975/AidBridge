package com.drc.aidbridge.modules.sponsor.internal.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "sponsor_profiles")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Sponsor {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "user_id", nullable = false)
	private UUID userId;

	@Column(name = "organization_name", length = 255)
	private String organizationName;

	@Column(name = "organization_type", length = 255)
	private String organizationType;

	@Column(name = "donation_count")
	private Integer donationCount;

	@Column(name = "total_donated_value", precision = 19, scale = 2)
	private BigDecimal totalDonatedValue;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;
}
