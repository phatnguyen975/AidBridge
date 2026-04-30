package com.drc.aidbridge.modules.mission.internal.usecase;

import com.drc.aidbridge.modules.mission.MissionHistoryDTO;
import com.drc.aidbridge.modules.mission.internal.repository.MissionJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class GetMissionHistoryUseCase {

    private final MissionJpaRepository missionRepository;

    @Transactional(readOnly = true)
    public Page<MissionHistoryDTO> execute(UUID volunteerId, Pageable pageable) {
        return missionRepository.findHistoryProjectionByVolunteerId(volunteerId, pageable)
                .map(projection -> MissionHistoryDTO.builder()
                        .missionType(projection.getMissionType())
                        .completedAt(projection.getCompletedAt())
                        .address(projection.getAddress())
                        .build());
    }
}
