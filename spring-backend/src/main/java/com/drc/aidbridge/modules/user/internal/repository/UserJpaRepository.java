package com.drc.aidbridge.modules.user.internal.repository;

import com.drc.aidbridge.entity.enums.UserRole;
import com.drc.aidbridge.modules.user.internal.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserJpaRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    Optional<User> findByPhoneNumber(String phoneNumber);

    boolean existsByEmail(String email);

    boolean existsByPhoneNumber(String phoneNumber);

    List<User> findByRole(UserRole role);

    List<User> findByRoleAndIsActiveTrue(UserRole role);

    Optional<User> findByEmailOrPhoneNumber(String email, String phoneNumber);
}
