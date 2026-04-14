package com.drc.aidbridge.modules.mission.internal.usecase;

import com.drc.aidbridge.modules.shared.enums.MissionStatus;
import com.drc.aidbridge.modules.shared.enums.SosStatus;
import com.drc.aidbridge.modules.shared.exception.ResourceNotFoundException;
import com.drc.aidbridge.modules.mission.internal.entity.Mission;
import com.drc.aidbridge.modules.mission.internal.cache.MissionCacheRedisSchema;
import com.drc.aidbridge.modules.mission.internal.mapper.MissionMapper;
import com.drc.aidbridge.modules.mission.internal.repository.MissionJpaRepository;
import com.drc.aidbridge.modules.mission.internal.web.dto.CompleteMissionRequest;
import com.drc.aidbridge.modules.mission.internal.web.dto.MissionResponse;
import com.drc.aidbridge.modules.sos.SosDTO;
import com.drc.aidbridge.modules.sos.SosFacade;
import com.drc.aidbridge.modules.user.UserDTO;
import com.drc.aidbridge.modules.user.UserFacade;
import com.drc.aidbridge.modules.notification.NotificationFacade;
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
    private final UserFacade userFacade;
    private final SosFacade sosFacade;
    private final MissionMapper missionMapper;
    private final MissionCacheRedisSchema missionCache;
    private final NotificationFacade notificationFacade;

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
        SosDTO sos = resolveSos(saved.getSosRequestId());
        return missionMapper.toResponseWithDetails(saved, volunteer, sos);
    }

    private boolean isCompletableStatus(MissionStatus status) {
        return status == MissionStatus.ASSIGNED ||
                status == MissionStatus.PICKING_UP ||
                status == MissionStatus.PICKED_UP ||
                status == MissionStatus.IN_TRANSIT;
    }

    private void updateRelatedRequestStatus(Mission mission) {
        if (mission.getSosRequestId() != null) {
            sosFacade.updateStatus(mission.getSosRequestId(), SosStatus.COMPLETED);
        }
    }

    private void sendCompletionNotification(Mission mission) {
        try {
            if (mission.getSosRequestId() != null) {
                String volunteerName = resolveVolunteerName(mission.getVolunteerId());
                notificationFacade.notifyMissionCompleted(mission.getId(), volunteerName);
                log.info("Completion notification prepared for mission {}", mission.getId());
            }
        } catch (Exception e) {
            log.error("Failed to send completion FCM for mission {}", mission.getId(), e);
        }
    }

    private UserDTO resolveVolunteer(UUID volunteerId) {
        if (volunteerId == null)
            return null;
        try {
            return userFacade.getUserById(volunteerId);
        } catch (Exception e) {
            return null;
        }
    }

    private String resolveVolunteerName(UUID volunteerId) {
        if (volunteerId == null)
            return "Tình nguyện viên";
        try {
            return userFacade.getUserById(volunteerId).getName();
        } catch (Exception e) {
            return "Tình nguyện viên";
        }
    }

    private SosDTO resolveSos(UUID sosRequestId) {
        if (sosRequestId == null)
            return null;
        return sosFacade.getSosRequestById(sosRequestId).orElse(null);
    }
}
