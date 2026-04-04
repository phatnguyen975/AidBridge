package com.drc.aidbridge.modules.mission.internal.usecase;

import com.drc.aidbridge.modules.mission.internal.cache.MissionCacheRedisSchema;
import com.drc.aidbridge.modules.mission.internal.entity.DispatchAttempt;
import com.drc.aidbridge.modules.mission.internal.entity.Mission;
import com.drc.aidbridge.modules.mission.internal.mapper.MissionMapper;
import com.drc.aidbridge.modules.mission.internal.repository.DispatchAttemptJpaRepository;
import com.drc.aidbridge.modules.mission.internal.repository.MissionJpaRepository;
import com.drc.aidbridge.modules.mission.internal.web.dto.AssignMissionRequest;
import com.drc.aidbridge.modules.mission.internal.web.dto.MissionResponse;
import com.drc.aidbridge.modules.shared.enums.DispatchResponse;
import com.drc.aidbridge.modules.shared.enums.DispatchType;
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
 * UseCase: Staff/Admin manually assign volunteer vào mission.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AssignMissionUseCase {

    private final MissionJpaRepository missionRepository;
    private final DispatchAttemptJpaRepository dispatchAttemptRepository;
    private final UserFacade userFacade;
    private final SosFacade sosFacade;
    private final MissionMapper missionMapper;
    private final MissionCacheRedisSchema missionCache;

    @Transactional
    public MissionResponse execute(UUID missionId, AssignMissionRequest request) {
        log.info("Manually assigning volunteer {} to mission {}", request.getVolunteerId(), missionId);

        Mission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> new ResourceNotFoundException("Mission not found: " + missionId));

        UUID volunteerId = request.getVolunteerId();

        // Validate mission status
        if (mission.getStatus() == MissionStatus.COMPLETED || mission.getStatus() == MissionStatus.CANCELLED) {
            throw new IllegalStateException("Cannot assign volunteer to completed/cancelled mission");
        }

        // Check if mission already assigned
        if (mission.getVolunteerId() != null) {
            throw new IllegalStateException("Mission already assigned to volunteer: " + mission.getVolunteerId());
        }

        // Kiểm tra volunteer tồn tại và available
        UserDTO volunteer = userFacade.getUserById(volunteerId);
        if (volunteer == null) {
            throw new ResourceNotFoundException("Volunteer not found: " + volunteerId);
        }

        // Kiểm tra volunteer không có mission active khác
        missionRepository.findActiveByVolunteerId(volunteerId).ifPresent(activeMission -> {
            throw new IllegalStateException("Volunteer already has an active mission: " + activeMission.getId());
        });

        // Tạo dispatch attempt với type MANUAL (giả lập SEQUENTIAL nhưng là manual)
        DispatchAttempt attempt = DispatchAttempt.builder()
                .missionId(missionId)
                .volunteerId(volunteerId)
                .dispatchType(DispatchType.SEQUENTIAL)
                .batchNumber(dispatchAttemptRepository.getNextBatchNumber(missionId))
                .response(DispatchResponse.ACCEPTED)
                .respondedAt(Instant.now())
                .build();
        dispatchAttemptRepository.save(attempt);

        // Update mission
        mission.setVolunteerId(volunteerId);
        mission.setStatus(MissionStatus.ASSIGNED);
        mission.setAcceptedAt(Instant.now());

        Mission saved = missionRepository.save(mission);
        log.info("Mission {} manually assigned to volunteer {}", missionId, volunteerId);

        // Invalidate cache
        missionCache.invalidateMissionCache(missionId);

        // Build response
        SosDTO sos = resolveSos(saved.getSosRequestId());
        return missionMapper.toResponseWithDetails(saved, volunteer, sos);
    }

    private SosDTO resolveSos(UUID sosRequestId) {
        if (sosRequestId == null)
            return null;
        return sosFacade.getSosRequestById(sosRequestId).orElse(null);
    }
}
