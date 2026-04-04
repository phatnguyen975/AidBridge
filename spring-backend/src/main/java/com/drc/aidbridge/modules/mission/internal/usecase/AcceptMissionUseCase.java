package com.drc.aidbridge.modules.mission.internal.usecase;

import com.drc.aidbridge.modules.mission.internal.cache.MissionCacheRedisSchema;
import com.drc.aidbridge.modules.mission.internal.entity.DispatchAttempt;
import com.drc.aidbridge.modules.mission.internal.entity.Mission;
import com.drc.aidbridge.modules.mission.internal.mapper.MissionMapper;
import com.drc.aidbridge.modules.mission.internal.repository.DispatchAttemptJpaRepository;
import com.drc.aidbridge.modules.mission.internal.repository.MissionJpaRepository;
import com.drc.aidbridge.modules.mission.internal.web.dto.AcceptMissionRequest;
import com.drc.aidbridge.modules.mission.internal.web.dto.MissionResponse;
import com.drc.aidbridge.modules.shared.enums.DispatchResponse;
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
 * UseCase: Volunteer chấp nhận dispatch request.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AcceptMissionUseCase {

    private final MissionJpaRepository missionRepository;
    private final DispatchAttemptJpaRepository dispatchAttemptRepository;
    private final UserFacade userFacade;
    private final SosFacade sosFacade;
    private final MissionMapper missionMapper;
    private final MissionCacheRedisSchema missionCache;

    @Transactional
    public MissionResponse execute(UUID missionId, UUID volunteerId, AcceptMissionRequest request) {
        log.info("Volunteer {} accepting mission {}", volunteerId, missionId);

        Mission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> new ResourceNotFoundException("Mission not found: " + missionId));

        // Validate mission status
        if (mission.getStatus() != MissionStatus.PENDING && mission.getStatus() != MissionStatus.DISPATCHING) {
            throw new IllegalStateException(
                    "Mission is not in dispatchable state. Current status: " + mission.getStatus());
        }

        // Check if mission already assigned to another volunteer
        if (mission.getVolunteerId() != null && !mission.getVolunteerId().equals(volunteerId)) {
            throw new IllegalStateException("Mission already assigned to another volunteer");
        }

        // Kiểm tra volunteer không có mission active khác
        missionRepository.findActiveByVolunteerId(volunteerId).ifPresent(activeMission -> {
            throw new IllegalStateException("Volunteer already has an active mission: " + activeMission.getId());
        });

        // Tìm và update dispatch attempt nếu có
        if (request.getDispatchAttemptId() != null) {
            DispatchAttempt attempt = dispatchAttemptRepository.findById(request.getDispatchAttemptId())
                    .orElseThrow(() -> new ResourceNotFoundException("Dispatch attempt not found"));

            if (attempt.getResponse() != DispatchResponse.PENDING) {
                throw new IllegalStateException("Dispatch attempt already responded");
            }

            attempt.setResponse(DispatchResponse.ACCEPTED);
            attempt.setRespondedAt(Instant.now());
            dispatchAttemptRepository.save(attempt);
        }

        // Update mission
        mission.setVolunteerId(volunteerId);
        mission.setStatus(MissionStatus.ASSIGNED);
        mission.setAcceptedAt(Instant.now());

        Mission saved = missionRepository.save(mission);
        log.info("Mission {} assigned to volunteer {}", missionId, volunteerId);

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
