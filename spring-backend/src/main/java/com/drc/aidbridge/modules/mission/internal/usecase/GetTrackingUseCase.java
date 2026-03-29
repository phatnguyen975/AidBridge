package com.drc.aidbridge.modules.mission.internal.usecase;

import com.drc.aidbridge.entity.enums.MissionStatus;
import com.drc.aidbridge.exception.ResourceNotFoundException;
import com.drc.aidbridge.modules.mission.internal.entity.Mission;
import com.drc.aidbridge.modules.mission.internal.mapper.MissionMapper;
import com.drc.aidbridge.modules.mission.internal.repository.MissionJpaRepository;
import com.drc.aidbridge.modules.mission.internal.web.dto.MissionTrackingResponse;
import com.drc.aidbridge.modules.user.UserDTO;
import com.drc.aidbridge.modules.user.UserFacade;
import com.drc.aidbridge.redis.MissionCacheRedisSchema;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class GetTrackingUseCase {

    private final MissionJpaRepository missionRepository;
    private final UserFacade userFacade;
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
            throw new IllegalStateException("Mission is not in a trackable state. Current: " + mission.getStatus());
        }

        UserDTO volunteer = resolveVolunteer(mission.getVolunteerId());
        String destinationAddress = getDestinationAddress(mission);

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

    private String getDestinationAddress(Mission mission) {
        if (mission.getSosRequest() != null) {
            return mission.getSosRequest().getAddress();
        }
        return null;
    }

    private UserDTO resolveVolunteer(UUID volunteerId) {
        if (volunteerId == null) return null;
        try { return userFacade.getUserById(volunteerId); }
        catch (Exception e) { return null; }
    }
}
