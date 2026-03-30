package com.drc.aidbridge.repository;

import com.drc.aidbridge.entity.User;
import com.drc.aidbridge.entity.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for User entity operations.
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Find user by email address.
     */
    Optional<User> findByEmail(String email);

    /**
     * Find user by phone number.
     */
    Optional<User> findByPhoneNumber(String phoneNumber);

    /**
     * Check if email is already registered.
     */
    boolean existsByEmail(String email);

    /**
     * Check if phone number is already registered.
     */
    boolean existsByPhoneNumber(String phoneNumber);

    /**
     * Find all users with a specific role.
     */
    List<User> findByRole(UserRole role);

    /**
     * Find all active users with a specific role.
     */
    List<User> findByRoleAndIsActiveTrue(UserRole role);

    /**
     * Find user by email or phone number.
     */
    Optional<User> findByEmailOrPhoneNumber(String email, String phoneNumber);
}
