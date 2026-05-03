package com.drc.aidbridge.modules.volunteer.internal.usecase;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import com.drc.aidbridge.modules.volunteer.internal.repository.VolunteerJpaRepository;
import com.drc.aidbridge.modules.volunteer.internal.entity.Volunteer;
import com.drc.aidbridge.modules.mission.MissionFacade;
import com.drc.aidbridge.modules.mission.DispatchAttemptDTO;
import com.drc.aidbridge.modules.shared.exception.ResourceNotFoundException;
import com.drc.aidbridge.modules.shared.exception.BadRequestException;

@Service
@RequiredArgsConstructor
public class AcceptDispatchAttemptUseCase {
    
    private final VolunteerJpaRepository volunteerRepository;
    private final MissionFacade missionFacade;
    
    @Transactional
    public DispatchAttemptDTO execute(UUID userId, UUID dispatchAttemptId) {
        // 1. Tìm volunteer và kiểm tra trạng thái
        Volunteer volunteer = volunteerRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Volunteer profile not found"));
        
        if (!volunteer.isOnline()) {
            throw new BadRequestException("Volunteer is not online");
        }
        
        // 2. Gửi yêu cầu cập nhật dispatch attempt qua MissionFacade
        return missionFacade.acceptDispatchAttempt(userId, dispatchAttemptId)
                .orElseThrow(() -> new BadRequestException("Failed to accept dispatch attempt. It may not exist, already be responded to, or does not belong to this volunteer."));
    }
}
