package com.drc.aidbridge.modules.staff.internal.repository;

import com.drc.aidbridge.modules.staff.internal.entity.Staff;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface StaffRepository extends JpaRepository<Staff, UUID> {
    Optional<Staff> findByUserId(UUID userId);
}
