package com.drc.aidbridge.modules.mission.internal.usecase;

import com.drc.aidbridge.modules.shared.exception.ResourceNotFoundException;
import com.drc.aidbridge.modules.mission.internal.entity.Mission;
import com.drc.aidbridge.modules.mission.internal.cache.MissionCacheRedisSchema;
import com.drc.aidbridge.modules.mission.internal.mapper.MissionMapper;
import com.drc.aidbridge.modules.mission.internal.repository.MissionJpaRepository;
import com.drc.aidbridge.modules.mission.internal.web.dto.MissionResponse;
import com.drc.aidbridge.modules.sos.SosDTO;
import com.drc.aidbridge.modules.sos.SosFacade;
import com.drc.aidbridge.modules.user.UserDTO;
import com.drc.aidbridge.modules.user.UserFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class GetMissionUseCase {

    private final MissionJpaRepository missionRepository;
    private final UserFacade userFacade;
    private final SosFacade sosFacade;
    private final MissionMapper missionMapper;
    private final MissionCacheRedisSchema missionCache;

    public MissionResponse execute(UUID missionId) {
        // Try cache first
        Optional<MissionResponse> cached = missionCache.getCachedMission(missionId);
        if (cached.isPresent()) {
            log.debug("Returning cached mission {}", missionId);
            return cached.get();
        }

        Mission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> new ResourceNotFoundException("Mission not found: " + missionId));

        // Resolve volunteer via facade
        UserDTO volunteer = resolveVolunteer(mission.getVolunteerId());
        SosDTO sos = resolveSos(mission.getSosRequestId());
        MissionResponse response = missionMapper.toResponseWithDetails(mission, volunteer, sos);

        missionCache.cacheMission(response);
        return response;
    }

    private UserDTO resolveVolunteer(UUID volunteerId) {
        if (volunteerId == null) return null;
        try {
            return userFacade.getUserById(volunteerId);
        } catch (Exception e) {
            log.warn("Could not resolve volunteer {}: {}", volunteerId, e.getMessage());
            return null;
        }
    }

    private SosDTO resolveSos(UUID sosRequestId) {
        if (sosRequestId == null) return null;
        return sosFacade.getSosRequestById(sosRequestId).orElse(null);
    }
}
