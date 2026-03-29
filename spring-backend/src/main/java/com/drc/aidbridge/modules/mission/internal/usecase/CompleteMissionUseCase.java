package com.drc.aidbridge.modules.mission.internal.usecase;

import com.drc.aidbridge.entity.SosRequest;
import com.drc.aidbridge.entity.enums.MissionStatus;
import com.drc.aidbridge.entity.enums.SosStatus;
import com.drc.aidbridge.exception.ResourceNotFoundException;
import com.drc.aidbridge.modules.mission.internal.entity.Mission;
import com.drc.aidbridge.modules.mission.internal.mapper.MissionMapper;
import com.drc.aidbridge.modules.mission.internal.repository.MissionJpaRepository;
import com.drc.aidbridge.modules.mission.internal.web.dto.CompleteMissionRequest;
import com.drc.aidbridge.modules.mission.internal.web.dto.MissionResponse;
import com.drc.aidbridge.modules.user.UserDTO;
import com.drc.aidbridge.modules.user.UserFacade;
import com.drc.aidbridge.redis.MissionCacheRedisSchema;
import com.drc.aidbridge.repository.SosRequestRepository;
import com.drc.aidbridge.service.FCMService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class CompleteMissionUseCase {

    private final MissionJpaRepository missionRepository;
    private final SosRequestRepository sosRequestRepository;
    private final UserFacade userFacade;
    private final MissionMapper missionMapper;
    private final MissionCacheRedisSchema missionCache;
    private final FCMService fcmService;

    @Transactional
    public MissionResponse execute(UUID missionId, MultipartFile confirmationImage, CompleteMissionRequest request) {
        Mission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> new ResourceNotFoundException("Mission not found: " + missionId));

        if (!isCompletableStatus(mission.getStatus())) {
            throw new IllegalStateException(
                    "Mission must be in an active status to complete. Current: " + mission.getStatus());
        }

        String imageUrl = null;
        if (confirmationImage != null && !confirmationImage.isEmpty()) {
            // TODO: Upload to cloud storage
            imageUrl = "/missions/" + missionId + "/confirmation.jpg";
        }

        mission.setStatus(MissionStatus.COMPLETED);
        mission.setCompletedAt(Instant.now());
        mission.setConfirmationImageUrl(imageUrl);
        if (request != null && request.getNotes() != null) {
            mission.setComment(request.getNotes());
        }

        Mission saved = missionRepository.save(mission);

        missionCache.removeMissionFromCache(missionId);

        sendCompletionNotification(saved);
        updateRelatedRequestStatus(saved);

        log.info("Mission {} completed", missionId);

        UserDTO volunteer = resolveVolunteer(saved.getVolunteerId());
        return missionMapper.toResponseWithDetails(saved, volunteer);
    }

    private boolean isCompletableStatus(MissionStatus status) {
        return status == MissionStatus.ASSIGNED ||
               status == MissionStatus.PICKING_UP ||
               status == MissionStatus.PICKED_UP ||
               status == MissionStatus.IN_TRANSIT;
    }

    private void updateRelatedRequestStatus(Mission mission) {
        if (mission.getSosRequest() != null) {
            SosRequest sos = mission.getSosRequest();
            sos.setStatus(SosStatus.COMPLETED);
            sosRequestRepository.save(sos);
        }
    }

    private void sendCompletionNotification(Mission mission) {
        try {
            if (mission.getSosRequest() != null) {
                String volunteerName = resolveVolunteerName(mission.getVolunteerId());
                FCMService.MissionNotification notification =
                        fcmService.createMissionCompletedNotification(mission.getId(), volunteerName);
                log.info("Completion notification prepared for mission {}", mission.getId());
            }
        } catch (Exception e) {
            log.error("Failed to send completion FCM for mission {}", mission.getId(), e);
        }
    }

    private UserDTO resolveVolunteer(UUID volunteerId) {
        if (volunteerId == null) return null;
        try { return userFacade.getUserById(volunteerId); }
        catch (Exception e) { return null; }
    }

    private String resolveVolunteerName(UUID volunteerId) {
        if (volunteerId == null) return "Tình nguyện viên";
        try { return userFacade.getUserById(volunteerId).getName(); }
        catch (Exception e) { return "Tình nguyện viên"; }
    }
}
