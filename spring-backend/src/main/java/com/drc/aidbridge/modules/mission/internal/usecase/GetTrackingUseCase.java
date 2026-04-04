package com.drc.aidbridge.modules.mission.internal.usecase;

import com.drc.aidbridge.modules.shared.enums.MissionStatus;
import com.drc.aidbridge.modules.shared.exception.InvalidMissionStateException;
import com.drc.aidbridge.modules.shared.exception.ResourceNotFoundException;
import com.drc.aidbridge.modules.mission.internal.entity.Mission;
import com.drc.aidbridge.modules.mission.internal.cache.MissionCacheRedisSchema;
import com.drc.aidbridge.modules.mission.internal.mapper.MissionMapper;
import com.drc.aidbridge.modules.mission.internal.repository.MissionJpaRepository;
import com.drc.aidbridge.modules.mission.internal.web.dto.MissionTrackingResponse;
import com.drc.aidbridge.modules.sos.SosDTO;
import com.drc.aidbridge.modules.sos.SosFacade;
import com.drc.aidbridge.modules.user.UserDTO;
import com.drc.aidbridge.modules.user.UserFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class GetTrackingUseCase {

    private final MissionJpaRepository missionRepository;
    private final UserFacade userFacade;
    private final SosFacade sosFacade;
    private final MissionMapper missionMapper;
    private final MissionCacheRedisSchema missionCache;

    public MissionTrackingResponse execute(UUID missionId) {
        Optional<MissionTrackingResponse> cached = missionCache.getCachedTracking(missionId);
        if (cached.isPresent()) {
            return cached.get();
        }

        Mission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> new ResourceNotFoundException("Mission not found: " + missionId));

        if (!isTrackableStatus(mission.getStatus())) {
            throw new InvalidMissionStateException(
                    "Mission is not in a trackable state. Current: " + mission.getStatus());
        }

        UserDTO volunteer = resolveVolunteer(mission.getVolunteerId());
        SosDTO sos = resolveSos(mission.getSosRequestId());
        String destinationAddress = getDestinationAddress(sos);

        // TODO: Implement real ETA and distance calculation
        MissionTrackingResponse tracking = missionMapper.toTrackingResponse(
                mission, volunteer, destinationAddress, null, null);

        missionCache.cacheTracking(tracking);
        return tracking;
    }

    private boolean isTrackableStatus(MissionStatus status) {
        return status == MissionStatus.ASSIGNED ||
                status == MissionStatus.PICKING_UP ||
                status == MissionStatus.PICKED_UP ||
                status == MissionStatus.IN_TRANSIT ||
                status == MissionStatus.DISPATCHING;
    }

    private String getDestinationAddress(SosDTO sos) {
        return sos != null ? sos.getAddress() : null;
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
