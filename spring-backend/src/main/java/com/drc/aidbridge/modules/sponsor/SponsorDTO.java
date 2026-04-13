package com.drc.aidbridge.modules.sponsor;

import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SponsorDTO {
	private UUID id;
	private UUID userId;
	private String organizationName;
	private String organizationType;
	private Integer donationCount;
	private BigDecimal totalDonatedValue;
	private Instant createdAt;
	private Instant updatedAt;
}
