package com.drc.aidbridge.modules.mission.internal.usecase;

import com.drc.aidbridge.modules.shared.enums.MissionStatus;
import com.drc.aidbridge.modules.shared.exception.ResourceNotFoundException;
import com.drc.aidbridge.modules.mission.internal.entity.Mission;
import com.drc.aidbridge.modules.mission.internal.cache.MissionCacheRedisSchema;
import com.drc.aidbridge.modules.mission.internal.mapper.MissionMapper;
import com.drc.aidbridge.modules.mission.internal.repository.MissionJpaRepository;
import com.drc.aidbridge.modules.mission.internal.web.dto.CancelMissionRequest;
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

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class CancelMissionUseCase {

    private final MissionJpaRepository missionRepository;
    private final UserFacade userFacade;
    private final SosFacade sosFacade;
    private final MissionMapper missionMapper;
    private final MissionCacheRedisSchema missionCache;
    private final NotificationFacade notificationFacade;

    @Transactional
    public MissionResponse execute(UUID missionId, CancelMissionRequest request) {
        Mission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> new ResourceNotFoundException("Mission not found: " + missionId));

        if (mission.getStatus() == MissionStatus.COMPLETED) {
            throw new IllegalStateException("Cannot cancel a completed mission");
        }
        if (mission.getStatus() == MissionStatus.CANCELLED) {
            throw new IllegalStateException("Mission is already cancelled");
        }

        mission.setStatus(MissionStatus.CANCELLED);
        mission.setCancelledAt(Instant.now());
        mission.setCancellationReason(request.getCancellationReason());

        Mission saved = missionRepository.save(mission);

        missionCache.removeMissionFromCache(missionId);

        sendCancellationNotification(saved);

        UserDTO volunteer = resolveVolunteer(saved.getVolunteerId());
        SosDTO sos = resolveSos(saved.getSosRequestId());
        return missionMapper.toResponseWithDetails(saved, volunteer, sos);
    }

    private void sendCancellationNotification(Mission mission) {
        try {
            if (mission.getSosRequestId() != null) {
                notificationFacade.notifyMissionCancelled(mission.getId(), mission.getCancellationReason());
                log.info("Cancellation notification prepared for mission {}", mission.getId());
            }
        } catch (Exception e) {
            log.error("Failed to send cancellation FCM for mission {}", mission.getId(), e);
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

    private SosDTO resolveSos(UUID sosRequestId) {
        if (sosRequestId == null)
            return null;
        return sosFacade.getSosRequestById(sosRequestId).orElse(null);
    }
}
