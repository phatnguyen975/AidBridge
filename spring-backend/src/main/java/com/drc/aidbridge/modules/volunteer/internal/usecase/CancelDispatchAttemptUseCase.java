package com.drc.aidbridge.modules.volunteer.internal.usecase;

import com.drc.aidbridge.modules.mission.DispatchAttemptDTO;
import com.drc.aidbridge.modules.mission.MissionFacade;
import com.drc.aidbridge.modules.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CancelDispatchAttemptUseCase {

    private final MissionFacade missionFacade;

    @Transactional
    public DispatchAttemptDTO execute(UUID volunteerId, UUID dispatchAttemptId) {
        DispatchAttemptDTO attempt = missionFacade.getDispatchAttempt(dispatchAttemptId)
                .orElseThrow(() -> new ResourceNotFoundException("Dispatch attempt not found"));

        if (!attempt.getVolunteerId().equals(volunteerId)) {
            throw new ResourceNotFoundException("Dispatch attempt not found");
        }

        return missionFacade.cancelDispatchAttempt(dispatchAttemptId).orElse(null);
    }
}
