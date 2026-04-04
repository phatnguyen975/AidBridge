package com.drc.aidbridge.modules.mission.internal.usecase;

import com.drc.aidbridge.modules.mission.internal.entity.Mission;
import com.drc.aidbridge.modules.mission.internal.mapper.MissionMapper;
import com.drc.aidbridge.modules.mission.internal.repository.MissionJpaRepository;
import com.drc.aidbridge.modules.mission.internal.web.dto.CreateMissionRequest;
import com.drc.aidbridge.modules.mission.internal.web.dto.MissionResponse;
import com.drc.aidbridge.modules.shared.enums.MissionStatus;
import com.drc.aidbridge.modules.shared.enums.MissionType;
import com.drc.aidbridge.modules.sos.SosDTO;
import com.drc.aidbridge.modules.sos.SosFacade;
import com.drc.aidbridge.modules.user.UserDTO;
import com.drc.aidbridge.modules.user.UserFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * UseCase: Tạo mission mới (Staff/Admin).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CreateMissionUseCase {

    private final MissionJpaRepository missionRepository;
    private final UserFacade userFacade;
    private final SosFacade sosFacade;
    private final MissionMapper missionMapper;

    @Transactional
    public MissionResponse execute(CreateMissionRequest request) {
        log.info("Creating new mission - type: {}, sosRequestId: {}", request.getMissionType(),
                request.getSosRequestId());

        // Build mission entity
        Mission mission = Mission.builder()
                .missionType(request.getMissionType())
                .sosRequestId(request.getSosRequestId())
                .aidRequestId(request.getAidRequestId())
                .helpRequestId(request.getHelpRequestId())
                .hubId(request.getHubId())
                .status(MissionStatus.PENDING)
                .priorityScore(request.getPriorityScore() != null ? request.getPriorityScore() : BigDecimal.ZERO)
                .victimLat(request.getVictimLat())
                .victimLng(request.getVictimLng())
                .comment(request.getComment())
                .build();

        // Nếu có volunteerIds, set volunteer đầu tiên và bắt đầu dispatch
        if (request.getVolunteerIds() != null && !request.getVolunteerIds().isEmpty()) {
            // Chỉ set volunteer nếu có 1 volunteer được chỉ định
            if (request.getVolunteerIds().size() == 1) {
                UUID volunteerId = request.getVolunteerIds().get(0);

                // Kiểm tra volunteer available
                missionRepository.findActiveByVolunteerId(volunteerId).ifPresent(activeMission -> {
                    log.warn("Volunteer {} already has active mission {}", volunteerId, activeMission.getId());
                });

                mission.setVolunteerId(volunteerId);
                mission.setStatus(MissionStatus.DISPATCHING);
            } else {
                // Multiple volunteers -> trigger broadcast dispatch (TODO)
                mission.setStatus(MissionStatus.DISPATCHING);
            }
        }

        Mission saved = missionRepository.save(mission);
        log.info("Mission created with id: {}", saved.getId());

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
