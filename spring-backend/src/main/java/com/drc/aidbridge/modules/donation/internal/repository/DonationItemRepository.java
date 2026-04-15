package com.drc.aidbridge.modules.donation.internal.repository;

import com.drc.aidbridge.modules.donation.internal.entity.DonationItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DonationItemRepository extends JpaRepository<DonationItem, UUID> {
    List<DonationItem> findAllByDonationId(UUID donationId);
}
