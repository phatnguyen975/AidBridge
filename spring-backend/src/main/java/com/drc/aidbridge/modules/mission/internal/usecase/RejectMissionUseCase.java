package com.drc.aidbridge.modules.mission.internal.usecase;

import com.drc.aidbridge.modules.mission.internal.entity.DispatchAttempt;
import com.drc.aidbridge.modules.mission.internal.entity.Mission;
import com.drc.aidbridge.modules.mission.internal.repository.DispatchAttemptJpaRepository;
import com.drc.aidbridge.modules.mission.internal.repository.MissionJpaRepository;
import com.drc.aidbridge.modules.mission.internal.web.dto.RejectMissionRequest;
import com.drc.aidbridge.modules.shared.enums.DispatchResponse;
import com.drc.aidbridge.modules.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * UseCase: Volunteer từ chối dispatch request.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RejectMissionUseCase {

    private final MissionJpaRepository missionRepository;
    private final DispatchAttemptJpaRepository dispatchAttemptRepository;

    @Transactional
    public void execute(UUID missionId, UUID volunteerId, RejectMissionRequest request) {
        log.info("Volunteer {} rejecting mission {}", volunteerId, missionId);

        Mission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> new ResourceNotFoundException("Mission not found: " + missionId));

        // Tìm dispatch attempt
        DispatchAttempt attempt;
        if (request.getDispatchAttemptId() != null) {
            attempt = dispatchAttemptRepository.findById(request.getDispatchAttemptId())
                    .orElseThrow(() -> new ResourceNotFoundException("Dispatch attempt not found"));
        } else {
            // Tìm dispatch attempt pending của volunteer cho mission này
            attempt = dispatchAttemptRepository
                    .findByMissionIdAndVolunteerIdAndResponse(missionId, volunteerId, DispatchResponse.PENDING)
                    .orElseThrow(() -> new ResourceNotFoundException("No pending dispatch attempt found"));
        }

        // Validate
        if (attempt.getResponse() != DispatchResponse.PENDING) {
            throw new IllegalStateException("Dispatch attempt already responded");
        }

        if (!attempt.getVolunteerId().equals(volunteerId)) {
            throw new IllegalStateException("Dispatch attempt does not belong to this volunteer");
        }

        // Update dispatch attempt
        attempt.setResponse(DispatchResponse.REJECTED);
        attempt.setRespondedAt(Instant.now());
        dispatchAttemptRepository.save(attempt);

        log.info("Mission {} rejected by volunteer {} - reason: {}", missionId, volunteerId, request.getReason());

        // TODO: Trigger re-dispatch to next volunteer nếu mission còn ở trạng thái
        // DISPATCHING
    }
}
