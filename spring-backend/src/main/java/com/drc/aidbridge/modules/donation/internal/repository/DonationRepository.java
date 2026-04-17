package com.drc.aidbridge.modules.donation.internal.repository;

import com.drc.aidbridge.modules.donation.internal.entity.Donation;
import com.drc.aidbridge.modules.shared.enums.DonationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface DonationRepository extends JpaRepository<Donation, UUID> {

	Page<Donation> findByStatus(DonationStatus status, Pageable pageable);

	Page<Donation> findByHubId(UUID hubId, Pageable pageable);

	Page<Donation> findByStatusAndHubId(DonationStatus status, UUID hubId, Pageable pageable);

}
