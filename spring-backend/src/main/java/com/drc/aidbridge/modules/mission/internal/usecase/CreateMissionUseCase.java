package com.drc.aidbridge.modules.mission.internal.usecase;

import com.drc.aidbridge.modules.mission.internal.entity.Mission;
import com.drc.aidbridge.modules.mission.internal.repository.MissionJpaRepository;
import com.drc.aidbridge.modules.mission.internal.web.dto.CreateMissionRequest;
import com.drc.aidbridge.modules.mission.internal.web.dto.MissionResponse;
import com.drc.aidbridge.modules.shared.enums.MissionStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class CreateMissionUseCase {

    private final MissionJpaRepository missionRepository;
    private final DispatchMissionUseCase dispatchMissionUseCase;

    @Transactional
    public MissionResponse execute(CreateMissionRequest request) {
        log.info("Creating new mission - type: {}, sosRequestId: {}", request.getMissionType(),
                request.getSosRequestId());

        Mission mission = Mission.builder()
                .missionType(request.getMissionType())
                .sosRequestId(request.getSosRequestId())
                .aidRequestId(request.getAidRequestId())
                .hubId(request.getHubId())
                .codeName(generateMissionCodeName())
                .status(MissionStatus.PENDING)
                .priorityScore(request.getPriorityScore() != null ? request.getPriorityScore() : BigDecimal.ZERO)
                .victimLocation(Mission.createPoint(request.getVictimLat(), request.getVictimLng()))
                .comment(request.getComment())
                .build();

        Mission saved = missionRepository.save(mission);
        log.info("Mission created with id: {}", saved.getId());

        return dispatchMissionUseCase.execute(saved.getId(), normalizePreferredVolunteerIds(request));
    }

    private List<UUID> normalizePreferredVolunteerIds(CreateMissionRequest request) {
        List<UUID> preferredVolunteerIds = new ArrayList<>();
        if (request.getVolunteerId() != null) {
            preferredVolunteerIds.add(request.getVolunteerId());
        }
        if (request.getVolunteerIds() != null && !request.getVolunteerIds().isEmpty()) {
            preferredVolunteerIds.addAll(request.getVolunteerIds());
        }
        return preferredVolunteerIds.stream().distinct().toList();
    }

    private String generateMissionCodeName() {
        String datePart = LocalDate.now(ZoneOffset.UTC).toString().replace("-", "");
        for (int i = 0; i < 10; i++) {
            String randomPart = String.format("%04d", ThreadLocalRandom.current().nextInt(0, 10000));
            String codeName = "MSN" + datePart + randomPart;
            if (!missionRepository.existsByCodeName(codeName)) {
                return codeName;
            }
        }
        throw new IllegalStateException("Unable to generate unique mission code name");
    }
}
