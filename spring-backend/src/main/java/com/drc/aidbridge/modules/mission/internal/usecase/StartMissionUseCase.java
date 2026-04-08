package com.drc.aidbridge.modules.mission.internal.usecase;

import com.drc.aidbridge.modules.mission.internal.cache.MissionCacheRedisSchema;
import com.drc.aidbridge.modules.mission.internal.entity.Mission;
import com.drc.aidbridge.modules.mission.internal.mapper.MissionMapper;
import com.drc.aidbridge.modules.mission.internal.repository.MissionJpaRepository;
import com.drc.aidbridge.modules.mission.internal.web.dto.MissionResponse;
import com.drc.aidbridge.modules.mission.internal.web.dto.StartMissionRequest;
import com.drc.aidbridge.modules.shared.enums.MissionStatus;
import com.drc.aidbridge.modules.shared.exception.ResourceNotFoundException;
import com.drc.aidbridge.modules.sos.SosDTO;
import com.drc.aidbridge.modules.sos.SosFacade;
import com.drc.aidbridge.modules.user.UserDTO;
import com.drc.aidbridge.modules.user.UserFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * UseCase: Volunteer bắt đầu thực hiện mission.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StartMissionUseCase {

    private final MissionJpaRepository missionRepository;
    private final UserFacade userFacade;
    private final SosFacade sosFacade;
    private final MissionMapper missionMapper;
    private final MissionCacheRedisSchema missionCache;

    @Transactional
    public MissionResponse execute(UUID missionId, UUID volunteerId, StartMissionRequest request) {
        log.info("Volunteer {} starting mission {}", volunteerId, missionId);

        Mission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> new ResourceNotFoundException("Mission not found: " + missionId));

        // Validate volunteer ownership
        if (!volunteerId.equals(mission.getVolunteerId())) {
            throw new IllegalStateException("Mission is not assigned to this volunteer");
        }

        // Validate mission status
        if (mission.getStatus() != MissionStatus.ASSIGNED) {
            throw new IllegalStateException(
                    "Mission must be in ASSIGNED status to start. Current: " + mission.getStatus());
        }

        // Update mission
        mission.setStatus(MissionStatus.PICKING_UP);
        mission.setStartedAt(Instant.now());

        Mission saved = missionRepository.save(mission);
        log.info("Mission {} started by volunteer {}", missionId, volunteerId);

        // Invalidate cache
        missionCache.invalidateMissionCache(missionId);

        // Build response
        UserDTO volunteer = resolveVolunteer(saved.getVolunteerId());
        SosDTO sos = resolveSos(saved.getSosRequestId());
        return missionMapper.toResponseWithDetails(saved, volunteer, sos);
    }

    private UserDTO resolveVolunteer(UUID volunteerId) {
        if (volunteerId == null)
            return null;
        try {
            return userFacade.getUserById(volunteerId);
        } catch (Exception e) {
            log.warn("Could not resolve volunteer {}: {}", volunteerId, e.getMessage());
            return null;
        }
    }

    private SosDTO resolveSos(UUID sosRequestId) {
        if (sosRequestId == null)
            return null;
        return sosFacade.getSosRequestById(sosRequestId).orElse(null);
    }
}
