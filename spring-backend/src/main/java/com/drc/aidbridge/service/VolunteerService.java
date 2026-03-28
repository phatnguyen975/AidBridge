package com.drc.aidbridge.service;

import com.drc.aidbridge.dto.request.UpdateVolunteerProfileRequestDto;
import com.drc.aidbridge.dto.request.ToggleVolunteerStatusRequestDto;
import com.drc.aidbridge.dto.request.UpdateVolunteerLocationRequestDto;
import com.drc.aidbridge.dto.response.VolunteerProfileDto;
import com.drc.aidbridge.dto.response.VolunteerProfileResponseDto;
import com.drc.aidbridge.dto.response.UserDto;
import com.drc.aidbridge.entity.Volunteer;
import com.drc.aidbridge.entity.User;
import com.drc.aidbridge.entity.enums.VehicleType;
import com.drc.aidbridge.exception.ResourceNotFoundException;
import com.drc.aidbridge.repository.VolunteerRepository;
import com.drc.aidbridge.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Service for volunteer profile management.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VolunteerService {

    private final VolunteerRepository volunteerRepository;
    private final UserRepository userRepository;

    /**
     * Get volunteer profile for authenticated user.
     *
     * @param userId The authenticated user ID
     * @return Volunteer profile response with user data
     * @throws ResourceNotFoundException if volunteer profile not found
     */
    @Transactional(readOnly = true)
    public VolunteerProfileResponseDto getVolunteerProfile(UUID userId) {
        // Get user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Get volunteer profile
        Volunteer volunteer = volunteerRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Volunteer profile not found"));

        // Map to DTOs
        VolunteerProfileDto profileDto = mapToProfileDto(volunteer);
        UserDto userDto = mapToUserDto(user);

        return VolunteerProfileResponseDto.builder()
                .profile(profileDto)
                .user(userDto)
                .build();
    }

    /**
     * Update volunteer profile (vehicle type).
     *
     * @param userId The authenticated user ID
     * @param request Update request containing new vehicle type
     * @return Updated volunteer profile response
     * @throws ResourceNotFoundException if volunteer profile not found
     */
    @Transactional
    public VolunteerProfileResponseDto updateVolunteerProfile(
            UUID userId,
            UpdateVolunteerProfileRequestDto request) {

        // Get user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Get volunteer profile
        Volunteer volunteer = volunteerRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Volunteer profile not found"));

        // Update vehicle type if provided
        if (request.getVehicleType() != null) {
            try {
                volunteer.setVehicleType(VehicleType.valueOf(request.getVehicleType().toUpperCase()));
                log.debug("Updated vehicle type for volunteer {}: {}",
                        userId, request.getVehicleType());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid vehicle type: " + request.getVehicleType());
            }
        }

        // Save updates
        volunteer = volunteerRepository.save(volunteer);

        // Map to DTOs
        VolunteerProfileDto profileDto = mapToProfileDto(volunteer);
        UserDto userDto = mapToUserDto(user);

        return VolunteerProfileResponseDto.builder()
                .profile(profileDto)
                .user(userDto)
                .build();
    }

    /**
     * Create volunteer profile for new volunteer user.
     *
     * @param userId The user ID
     * @return Created volunteer profile
     */
    @Transactional
    public Volunteer createVolunteerProfile(UUID userId) {
        Volunteer volunteer = Volunteer.builder()
                .userId(userId)
                .isOnline(false)
                .totalTasksCompleted(0)
                .build();

        volunteer = volunteerRepository.save(volunteer);
        log.info("Created volunteer profile for user: {}", userId);

        return volunteer;
    }

    /**
     * Toggle volunteer online status and update location coordinates.
     *
     * @param userId The authenticated user ID
     * @param request Status toggle request containing is_online and optional coordinates
     * @return Updated volunteer profile response
     * @throws ResourceNotFoundException if volunteer profile not found
     */
    @Transactional
    public VolunteerProfileResponseDto toggleVolunteerStatus(
            UUID userId,
            ToggleVolunteerStatusRequestDto request) {

        // Get user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Get volunteer profile
        Volunteer volunteer = volunteerRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Volunteer profile not found"));

        // Update online status
        volunteer.setOnline(request.isOnline());

        // Update location if provided
        if (request.getCurrentLat() != null) {
            volunteer.setCurrentLat(request.getCurrentLat());
        }
        if (request.getCurrentLng() != null) {
            volunteer.setCurrentLng(request.getCurrentLng());
        }

        volunteer = volunteerRepository.save(volunteer);
        log.debug("Toggled volunteer {} online status to: {}, location: ({}, {})",
                userId, request.isOnline(), request.getCurrentLat(), request.getCurrentLng());

        // Map to DTOs
        VolunteerProfileDto profileDto = mapToProfileDto(volunteer);
        UserDto userDto = mapToUserDto(user);

        return VolunteerProfileResponseDto.builder()
                .profile(profileDto)
                .user(userDto)
                .build();
    }

    /**
     * Update volunteer current location.
     *
     * @param userId The authenticated user ID
     * @param request Location update request containing current_lat and current_lng
     * @throws ResourceNotFoundException if volunteer profile not found
     */
    @Transactional
    public void updateVolunteerLocation(
            UUID userId,
            UpdateVolunteerLocationRequestDto request) {

        // Get volunteer profile
        Volunteer volunteer = volunteerRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Volunteer profile not found"));

        // Update location
        volunteer.setCurrentLat(request.getCurrentLat());
        volunteer.setCurrentLng(request.getCurrentLng());

        volunteerRepository.save(volunteer);
        log.debug("Updated location for volunteer {}: ({}, {})",
                userId, request.getCurrentLat(), request.getCurrentLng());
    }

    // ==================== HELPER METHODS ====================

    private VolunteerProfileDto mapToProfileDto(Volunteer volunteer) {
        return VolunteerProfileDto.builder()
                .id(volunteer.getId())
                .userId(volunteer.getUserId())
                .isOnline(volunteer.isOnline())
                .currentLat(volunteer.getCurrentLat())
                .currentLng(volunteer.getCurrentLng())
                .vehicleType(volunteer.getVehicleType() != null
                        ? volunteer.getVehicleType().name()
                        : null)
                .totalTasksCompleted(volunteer.getTotalTasksCompleted())
                .avgRating(volunteer.getAvgRating())
                .avgResponseSeconds(volunteer.getAvgResponseSeconds())
                .createdAt(volunteer.getCreatedAt())
                .updatedAt(volunteer.getUpdatedAt())
                .build();
    }

    private UserDto mapToUserDto(User user) {
        return UserDto.builder()
                .id(user.getId().toString())
                .name(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhoneNumber())
                .role(user.getRole().name())
                .avatarUrl(user.getAvatarUrl())
                .isVerified(user.isVerified())
                .build();
    }
}
