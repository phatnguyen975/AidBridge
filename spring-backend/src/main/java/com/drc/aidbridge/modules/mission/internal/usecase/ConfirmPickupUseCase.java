package com.drc.aidbridge.modules.mission.internal.usecase;

import com.drc.aidbridge.entity.enums.MissionStatus;
import com.drc.aidbridge.entity.enums.MissionType;
import com.drc.aidbridge.exception.ResourceNotFoundException;
import com.drc.aidbridge.modules.mission.internal.entity.Mission;
import com.drc.aidbridge.modules.mission.internal.mapper.MissionMapper;
import com.drc.aidbridge.modules.mission.internal.repository.MissionJpaRepository;
import com.drc.aidbridge.modules.mission.internal.web.dto.ConfirmPickupRequest;
import com.drc.aidbridge.modules.mission.internal.web.dto.MissionResponse;
import com.drc.aidbridge.modules.user.UserDTO;
import com.drc.aidbridge.modules.user.UserFacade;
import com.drc.aidbridge.redis.MissionCacheRedisSchema;
import com.drc.aidbridge.service.FCMService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class ConfirmPickupUseCase {

    private final MissionJpaRepository missionRepository;
    private final UserFacade userFacade;
    private final MissionMapper missionMapper;
    private final MissionCacheRedisSchema missionCache;
    private final FCMService fcmService;

    @Transactional
    public MissionResponse execute(UUID missionId, ConfirmPickupRequest request) {
        Mission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> new ResourceNotFoundException("Mission not found: " + missionId));

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

        mission.setStatus(MissionStatus.PICKED_UP);
        mission.setPickedUpAt(Instant.now());
        Mission saved = missionRepository.save(mission);

        missionCache.invalidateMissionCache(missionId);

        sendPickupNotification(saved);

        UserDTO volunteer = resolveVolunteer(saved.getVolunteerId());
        MissionResponse response = missionMapper.toResponseWithDetails(saved, volunteer);
        missionCache.cacheMission(response);
        return response;
    }

    private void sendPickupNotification(Mission mission) {
        try {
            if (mission.getSosRequest() != null) {
                UserDTO victim = resolveUser(mission.getSosRequest().getRequesterId());
                if (victim != null) {
                    String volunteerName = resolveVolunteerName(mission.getVolunteerId());
                    // FCM notification uses infrastructure service directly
                    FCMService.MissionNotification notification =
                            fcmService.createPickupConfirmedNotification(mission.getId(), volunteerName);
                    // TODO: Get FCM token from user module when UserDTO exposes it
                    log.info("Pickup notification prepared for mission {}", mission.getId());
                }
            }
        } catch (Exception e) {
            log.error("Failed to send pickup FCM for mission {}", mission.getId(), e);
        }
    }

    private UserDTO resolveVolunteer(UUID volunteerId) {
        if (volunteerId == null) return null;
        try { return userFacade.getUserById(volunteerId); }
        catch (Exception e) { return null; }
    }

    private UserDTO resolveUser(UUID userId) {
        if (userId == null) return null;
        try { return userFacade.getUserById(userId); }
        catch (Exception e) { return null; }
    }

    private String resolveVolunteerName(UUID volunteerId) {
        if (volunteerId == null) return "Tình nguyện viên";
        try { return userFacade.getUserById(volunteerId).getName(); }
        catch (Exception e) { return "Tình nguyện viên"; }
    }
}
