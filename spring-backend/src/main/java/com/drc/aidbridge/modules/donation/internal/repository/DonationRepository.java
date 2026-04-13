package com.drc.aidbridge.modules.donation.internal.repository;

import com.drc.aidbridge.modules.donation.internal.entity.Donation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface DonationRepository extends JpaRepository<Donation, UUID> {

}
