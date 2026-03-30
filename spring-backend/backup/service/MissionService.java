package com.drc.aidbridge.service;

import com.drc.aidbridge.dto.request.CancelMissionRequestDto;
import com.drc.aidbridge.dto.request.CompleteMissionRequestDto;
import com.drc.aidbridge.dto.request.ConfirmPickupRequestDto;
import com.drc.aidbridge.dto.response.MissionListResponseDto;
import com.drc.aidbridge.dto.response.MissionResponseDto;
import com.drc.aidbridge.dto.response.MissionTrackingResponseDto;
import com.drc.aidbridge.dto.response.SosRequestResponseDto;
import com.drc.aidbridge.entity.Mission;
import com.drc.aidbridge.entity.SosRequest;
import com.drc.aidbridge.entity.User;
import com.drc.aidbridge.entity.enums.MissionStatus;
import com.drc.aidbridge.entity.enums.MissionType;
import com.drc.aidbridge.exception.ResourceNotFoundException;
import com.drc.aidbridge.redis.MissionCacheRedisSchema;
import com.drc.aidbridge.repository.MissionRepository;
import com.drc.aidbridge.repository.SosRequestRepository;
import com.drc.aidbridge.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MissionService {

    private final MissionRepository missionRepository;
    private final SosRequestRepository sosRequestRepository;
    private final UserRepository userRepository;
    private final MissionCacheRedisSchema missionCache;
    private final FCMService fcmService;

    // ==================== GET /missions ====================

    /**
     * List missions with optional filters and pagination.
     * Cache: Not cached - list data changes frequently.
     */
    public MissionListResponseDto listMissions(
            MissionType missionType,
            MissionStatus status,
            UUID volunteerId,
            int page,
            int limit) {

        Pageable pageable = PageRequest.of(page - 1, limit, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Mission> missionPage;
        if (missionType != null && status != null && volunteerId != null) {
            missionPage = missionRepository.findByMissionTypeAndStatusAndVolunteerId(
                    missionType, status, volunteerId, pageable);
        } else if (missionType != null && status != null) {
            missionPage = missionRepository.findByMissionTypeAndStatus(missionType, status, pageable);
        } else if (missionType != null && volunteerId != null) {
            missionPage = missionRepository.findByMissionTypeAndVolunteerId(missionType, volunteerId, pageable);
        } else if (status != null && volunteerId != null) {
            missionPage = missionRepository.findByStatusAndVolunteerId(status, volunteerId, pageable);
        } else if (missionType != null) {
            missionPage = missionRepository.findByMissionType(missionType, pageable);
        } else if (status != null) {
            missionPage = missionRepository.findByStatus(status, pageable);
        } else if (volunteerId != null) {
            missionPage = missionRepository.findByVolunteerId(volunteerId, pageable);
        } else {
            missionPage = missionRepository.findAll(pageable);
        }

        List<MissionResponseDto> items = missionPage.getContent().stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());

        MissionListResponseDto.PaginationDto pagination = MissionListResponseDto.PaginationDto.builder()
                .page(page)
                .limit(limit)
                .total(missionPage.getTotalElements())
                .totalPages(missionPage.getTotalPages())
                .hasNext(missionPage.hasNext())
                .hasPrevious(missionPage.hasPrevious())
                .build();

        return MissionListResponseDto.builder()
                .items(items)
                .pagination(pagination)
                .build();
    }

    // ==================== GET /missions/{id} ====================

    /**
     * Get mission by ID with caching.
     * Cache: Redis with TTL 5 minutes for active missions.
     */
    public MissionResponseDto getMission(UUID missionId) {
        // Try cache first
        Optional<MissionResponseDto> cached = missionCache.getCachedMission(missionId);
        if (cached.isPresent()) {
            log.debug("Returning cached mission {}", missionId);
            return cached.get();
        }

        // Cache miss - fetch from database
        Mission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> new ResourceNotFoundException("Mission not found: " + missionId));

        MissionResponseDto response = mapToResponseDtoWithDetails(mission);

        // Cache if active
        missionCache.cacheMission(response);

        return response;
    }

    // ==================== POST /missions/{id}/pickup ====================

    /**
     * Confirm pickup at hub (for DELIVERY missions).
     * Cache: Invalidates mission cache after successful pickup.
     */
    @Transactional
    public MissionResponseDto confirmPickup(UUID missionId, ConfirmPickupRequestDto request) {
        Mission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> new ResourceNotFoundException("Mission not found: " + missionId));

        // Validate mission type and status
        if (mission.getMissionType() != MissionType.DELIVERY) {
            throw new IllegalStateException("Only DELIVERY missions support pickup confirmation");
        }
        if (mission.getStatus() != MissionStatus.ASSIGNED) {
            throw new IllegalStateException("Mission must be in ASSIGNED status for pickup. Current: " + mission.getStatus());
        }
                    

        // Optional QR code verification
        if (request.getQrCodeToken() != null && !request.getQrCodeToken().isEmpty()) {
            if (!request.getQrCodeToken().equals(mission.getQrCodeToken())) {
                throw new IllegalArgumentException("Invalid QR code token");
            }
        }

        // Update mission status
        mission.setStatus(MissionStatus.PICKED_UP);
        mission.setPickedUpAt(Instant.now());
        Mission savedMission = missionRepository.save(mission);

        // Invalidate cache
        missionCache.invalidateMissionCache(missionId);

        // Send FCM notification to victim
        sendPickupConfirmedNotification(savedMission);

        
        MissionResponseDto response = mapToResponseDtoWithDetails(savedMission);
        missionCache.cacheMission(response);

        return response;
    }

    // ==================== POST /missions/{id}/complete ====================

    /**
     * Complete mission with confirmation image.
     * Cache: Removes mission from active cache.
     */
    @Transactional
    public MissionResponseDto completeMission(
            UUID missionId,
            MultipartFile confirmationImage,
            CompleteMissionRequestDto request) {

        Mission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> new ResourceNotFoundException("Mission not found: " + missionId));

        // Validate status - must be in trackable state
        if (!isCompletableStatus(mission.getStatus())) {
            throw new IllegalStateException(
                    "Mission must be in an active status to complete. Current: " + mission.getStatus());
        }

        // Handle confirmation image upload
        String imageUrl = null;
        if (confirmationImage != null && !confirmationImage.isEmpty()) {
            // TODO: Upload to cloud storage and get URL
            // imageUrl = storageService.upload(confirmationImage, "missions/" + missionId);
            imageUrl = "/missions/" + missionId + "/confirmation.jpg"; // Placeholder
        }

        // Update mission
        mission.setStatus(MissionStatus.COMPLETED);
        mission.setCompletedAt(Instant.now());
        mission.setConfirmationImageUrl(imageUrl);
        if (request != null && request.getNotes() != null) {
            mission.setComment(request.getNotes());
        }

        Mission savedMission = missionRepository.save(mission);

        // Remove from cache (completed missions are not cached)
        missionCache.removeMissionFromCache(missionId);

        // Send FCM notification to victim
        sendMissionCompletedNotification(savedMission);

        updateRelatedRequestStatus(savedMission);

        log.info("Mission {} completed", missionId);

        return mapToResponseDtoWithDetails(savedMission);
    }

    // ==================== POST /missions/{id}/cancel ====================

    /**
     * Cancel mission.
     * Cache: Removes mission from active cache.
     */
    @Transactional
    public MissionResponseDto cancelMission(UUID missionId, CancelMissionRequestDto request) {
        Mission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> new ResourceNotFoundException("Mission not found: " + missionId));

        // Validate - cannot cancel already completed or cancelled missions
        if (mission.getStatus() == MissionStatus.COMPLETED) {
            throw new IllegalStateException("Cannot cancel a completed mission");
        }
        if (mission.getStatus() == MissionStatus.CANCELLED) {
            throw new IllegalStateException("Mission is already cancelled");
        }

        // Update mission
        mission.setStatus(MissionStatus.CANCELLED);
        mission.setCancelledAt(Instant.now());
        mission.setCancellationReason(request.getCancellationReason());

        Mission savedMission = missionRepository.save(mission);

        // Remove from cache
        missionCache.removeMissionFromCache(missionId);

        // Send FCM notification to victim
        sendMissionCancelledNotification(savedMission);

        
        return mapToResponseDtoWithDetails(savedMission);
    }

    // ==================== GET /missions/{id}/tracking ====================

    /**
     * Get real-time tracking info.
     * Cache: Volunteer location from Redis geo data.
     */
    public MissionTrackingResponseDto getTracking(UUID missionId) {
        // Try tracking cache first
        Optional<MissionTrackingResponseDto> cached = missionCache.getCachedTracking(missionId);
        if (cached.isPresent()) {
            return cached.get();
        }

        Mission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> new ResourceNotFoundException("Mission not found: " + missionId));

        // Validate - only active missions can be tracked
        if (!isTrackableStatus(mission.getStatus())) {
            throw new IllegalStateException("Mission is not in a trackable state. Current: " + mission.getStatus());
        }

        // Build tracking response
        MissionTrackingResponseDto.VolunteerLocationDto volunteerLocation = null;
        if (mission.getVolunteerId() != null) {
            User volunteer = userRepository.findById(mission.getVolunteerId()).orElse(null);
            if (volunteer != null) {
                volunteerLocation = MissionTrackingResponseDto.VolunteerLocationDto.builder()
                        .id(volunteer.getId())
                        .fullName(volunteer.getFullName())
                        .avatarUrl(volunteer.getAvatarUrl())
                        // TODO: Get real-time location from Redis volunteer:locations geo data
                        .currentLat(null)
                        .currentLng(null)
                        .lastUpdated(Instant.now())
                        .build();
            }
        }

        MissionTrackingResponseDto.DestinationDto destination = null;
        if (mission.getVictimLat() != null && mission.getVictimLng() != null) {
            destination = MissionTrackingResponseDto.DestinationDto.builder()
                    .lat(mission.getVictimLat().doubleValue())
                    .lng(mission.getVictimLng().doubleValue())
                    .build();
        }

        MissionTrackingResponseDto tracking = MissionTrackingResponseDto.builder()
                .missionId(missionId)
                .status(mission.getStatus())
                .volunteer(volunteerLocation)
                .destination(destination)
                .destinationAddress(getDestinationAddress(mission))
                .etaMinutes(calculateETA(mission))
                .distanceKm(calculateDistance(mission))
                .websocketChannel("missions:" + missionId)
                .build();

        // Cache tracking data
        missionCache.cacheTracking(tracking);

        return tracking;
    }

    // ==================== Helper Methods ====================

    private MissionResponseDto mapToResponseDto(Mission mission) {
        return MissionResponseDto.builder()
                .id(mission.getId())
                .missionType(mission.getMissionType())
                .status(mission.getStatus())
                .sosRequestId(mission.getSosRequest() != null ? mission.getSosRequest().getId() : null)
                .aidRequestId(mission.getAidRequestId())
                .helpRequestId(mission.getHelpRequestId())
                .volunteerId(mission.getVolunteerId())
                .hubId(mission.getHubId())
                .victimLat(mission.getVictimLat())
                .victimLng(mission.getVictimLng())
                .qrCodeToken(mission.getQrCodeToken())
                .priorityScore(mission.getPriorityScore())
                .cancellationReason(mission.getCancellationReason())
                .confirmationImageUrl(mission.getConfirmationImageUrl())
                .imageUrl(mission.getImageUrl())
                .comment(mission.getComment())
                .acceptedAt(mission.getAcceptedAt())
                .pickedUpAt(mission.getPickedUpAt())
                .startedAt(mission.getStartedAt())
                .completedAt(mission.getCompletedAt())
                .cancelledAt(mission.getCancelledAt())
                .createdAt(mission.getCreatedAt())
                .updatedAt(mission.getUpdatedAt())
                .build();
    }

    private MissionResponseDto mapToResponseDtoWithDetails(Mission mission) {
        MissionResponseDto response = mapToResponseDto(mission);

        // Add volunteer details
        if (mission.getVolunteerId() != null) {
            userRepository.findById(mission.getVolunteerId()).ifPresent(volunteer -> {
                response.setVolunteer(MissionResponseDto.VolunteerBriefDto.builder()
                        .id(volunteer.getId())
                        .fullName(volunteer.getFullName())
                        .phoneNumber(volunteer.getPhoneNumber())
                        .avatarUrl(volunteer.getAvatarUrl())
                        .build());
            });
        }

        // Add SOS request details
        if (mission.getSosRequest() != null) {
            SosRequest sos = mission.getSosRequest();
            response.setSosRequest(SosRequestResponseDto.builder()
                    .id(sos.getId())
                    .requesterId(sos.getRequesterId())
                    .lat(sos.getLat())
                    .lng(sos.getLng())
                    .address(sos.getAddress())
                    .description(sos.getDescription())
                    .peopleCount(sos.getPeopleCount())
                    .urgencyLevel(sos.getUrgencyLevel())
                    .status(sos.getStatus())
                    .imageUrl(sos.getImageUrl())
                    .createdAt(sos.getCreatedAt())
                    .updatedAt(sos.getUpdatedAt())
                    .build());
        }

        return response;
    }

    private boolean isCompletableStatus(MissionStatus status) {
        return status == MissionStatus.ASSIGNED ||
               status == MissionStatus.PICKING_UP ||
               status == MissionStatus.PICKED_UP ||
               status == MissionStatus.IN_TRANSIT;
    }

    private boolean isTrackableStatus(MissionStatus status) {
        return status == MissionStatus.ASSIGNED ||
               status == MissionStatus.PICKING_UP ||
               status == MissionStatus.PICKED_UP ||
               status == MissionStatus.IN_TRANSIT ||
               status == MissionStatus.DISPATCHING;
    }

    private void updateRelatedRequestStatus(Mission mission) {
        if (mission.getSosRequest() != null) {
            SosRequest sos = mission.getSosRequest();
            sos.setStatus(com.drc.aidbridge.entity.enums.SosStatus.COMPLETED);
            sosRequestRepository.save(sos);
        }
        // TODO: Update aid_request status if applicable
    }

    private String getDestinationAddress(Mission mission) {
        if (mission.getSosRequest() != null) {
            return mission.getSosRequest().getAddress();
        }
        // TODO: Get address from aid_request if applicable
        return null;
    }

    private Integer calculateETA(Mission mission) {
        // TODO: Implement ETA calculation using volunteer location and destination
        return null;
    }

    private Double calculateDistance(Mission mission) {
        // TODO: Implement distance calculation using volunteer location and destination
        return null;
    }

    // ==================== FCM Notification Helper Methods ====================

    /**
     * Send pickup confirmed notification to victim.
     */
    private void sendPickupConfirmedNotification(Mission mission) {
        try {
            // Get victim (SOS requester) FCM token
            if (mission.getSosRequest() != null) {
                userRepository.findById(mission.getSosRequest().getRequesterId()).ifPresent(victim -> {
                    if (victim.getFcmToken() != null && !victim.getFcmToken().isEmpty()) {
                        // Get volunteer name
                        String volunteerName = getVolunteerName(mission.getVolunteerId());

                        FCMService.MissionNotification notification =
                                fcmService.createPickupConfirmedNotification(mission.getId(), volunteerName);

                        fcmService.sendMissionNotification(victim.getFcmToken(), notification);
                        log.info("Pickup confirmation FCM sent to victim {} for mission {}",
                                victim.getId(), mission.getId());
                    }
                });
            }
        } catch (Exception e) {
            log.error("Failed to send pickup confirmation FCM for mission {}", mission.getId(), e);
        }
    }

    /**
     * Send mission completed notification to victim.
     */
    private void sendMissionCompletedNotification(Mission mission) {
        try {
            // Get victim (SOS requester) FCM token
            if (mission.getSosRequest() != null) {
                userRepository.findById(mission.getSosRequest().getRequesterId()).ifPresent(victim -> {
                    if (victim.getFcmToken() != null && !victim.getFcmToken().isEmpty()) {
                        // Get volunteer name
                        String volunteerName = getVolunteerName(mission.getVolunteerId());

                        FCMService.MissionNotification notification =
                                fcmService.createMissionCompletedNotification(mission.getId(), volunteerName);

                        fcmService.sendMissionNotification(victim.getFcmToken(), notification);
                        log.info("Mission completion FCM sent to victim {} for mission {}",
                                victim.getId(), mission.getId());
                    }
                });
            }
        } catch (Exception e) {
            log.error("Failed to send mission completion FCM for mission {}", mission.getId(), e);
        }
    }

    /**
     * Send mission cancelled notification to victim.
     */
    private void sendMissionCancelledNotification(Mission mission) {
        try {
            // Get victim (SOS requester) FCM token
            if (mission.getSosRequest() != null) {
                userRepository.findById(mission.getSosRequest().getRequesterId()).ifPresent(victim -> {
                    if (victim.getFcmToken() != null && !victim.getFcmToken().isEmpty()) {
                        FCMService.MissionNotification notification =
                                fcmService.createMissionCancelledNotification(
                                        mission.getId(), mission.getCancellationReason());

                        fcmService.sendMissionNotification(victim.getFcmToken(), notification);
                        log.info("Mission cancellation FCM sent to victim {} for mission {}",
                                victim.getId(), mission.getId());
                    }
                });
            }
        } catch (Exception e) {
            log.error("Failed to send mission cancellation FCM for mission {}", mission.getId(), e);
        }
    }

    /**
     * Get volunteer name by ID.
     */
    private String getVolunteerName(UUID volunteerId) {
        if (volunteerId == null) {
            return "Tình nguyện viên";
        }
        return userRepository.findById(volunteerId)
                .map(User::getFullName)
                .orElse("Tình nguyện viên");
    }
}
