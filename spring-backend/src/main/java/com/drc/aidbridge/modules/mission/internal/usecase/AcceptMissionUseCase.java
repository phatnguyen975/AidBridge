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

        if (mission.getStatus() != MissionStatus.PENDING && mission.getStatus() != MissionStatus.DISPATCHING) {
            throw new IllegalStateException(
                    "Mission is not in dispatchable state. Current status: " + mission.getStatus());
        }

        if (mission.getVolunteerId() != null && !mission.getVolunteerId().equals(volunteerId)) {
            throw new IllegalStateException("Mission already assigned to another volunteer");
        }

        missionRepository.findActiveByVolunteerId(volunteerId).ifPresent(activeMission -> {
            throw new IllegalStateException("Volunteer already has an active mission: " + activeMission.getId());
        });

        DispatchAttempt attempt = resolveDispatchAttempt(missionId, volunteerId, mission.getStatus(), request);
        if (attempt != null) {
            attempt.setResponse(DispatchResponse.ACCEPTED);
            attempt.setRespondedAt(Instant.now());
            dispatchAttemptRepository.save(attempt);
        }

        mission.setVolunteerId(volunteerId);
        mission.setStatus(MissionStatus.ASSIGNED);
        mission.setAcceptedAt(Instant.now());

        Mission saved = missionRepository.save(mission);
        
        // Cập nhật tất cả các dispatch attempts khác của mission này thành TIMEOUT
        dispatchAttemptRepository.markOtherAttemptsAsTimeout(
                missionId, 
                volunteerId, 
                DispatchResponse.TIMEOUT, 
                DispatchResponse.PENDING, 
                Instant.now());

        missionCache.invalidateMissionCache(missionId);

        UserDTO volunteer = resolveVolunteer(saved.getVolunteerId());
        SosDTO sos = resolveSos(saved.getSosRequestId());
        return missionMapper.toResponseWithDetails(saved, volunteer, sos);
    }

    private DispatchAttempt resolveDispatchAttempt(UUID missionId, UUID volunteerId, MissionStatus missionStatus,
                                                   AcceptMissionRequest request) {
        DispatchAttempt attempt = null;
        if (request.getDispatchAttemptId() != null) {
            attempt = dispatchAttemptRepository.findById(request.getDispatchAttemptId())
                    .orElseThrow(() -> new ResourceNotFoundException("Dispatch attempt not found"));
        } else if (missionStatus == MissionStatus.DISPATCHING) {
            attempt = dispatchAttemptRepository
                    .findByMissionIdAndVolunteerIdAndResponse(missionId, volunteerId, DispatchResponse.PENDING)
                    .orElseThrow(() -> new IllegalStateException("Pending dispatch attempt required to accept mission"));
        }

        if (attempt == null) {
            return null;
        }

        if (!attempt.getVolunteerId().equals(volunteerId)) {
            throw new IllegalStateException("Dispatch attempt does not belong to this volunteer");
        }

        if (attempt.getResponse() != DispatchResponse.PENDING) {
            throw new IllegalStateException("Dispatch attempt already responded");
        }

        return attempt;
    }

    private UserDTO resolveVolunteer(UUID volunteerId) {
        if (volunteerId == null) {
            return null;
        }
        try {
            return userFacade.getUserById(volunteerId);
        } catch (Exception e) {
            log.warn("Could not resolve volunteer {}: {}", volunteerId, e.getMessage());
            return null;
        }
    }

    private SosDTO resolveSos(UUID sosRequestId) {
        if (sosRequestId == null) {
            return null;
        }
        return sosFacade.getSosRequestById(sosRequestId).orElse(null);
    }
}
