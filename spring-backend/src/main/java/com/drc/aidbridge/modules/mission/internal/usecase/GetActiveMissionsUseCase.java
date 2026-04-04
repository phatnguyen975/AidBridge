package com.drc.aidbridge.modules.mission.internal.usecase;

import com.drc.aidbridge.modules.mission.internal.entity.Mission;
import com.drc.aidbridge.modules.mission.internal.mapper.MissionMapper;
import com.drc.aidbridge.modules.mission.internal.repository.MissionJpaRepository;
import com.drc.aidbridge.modules.mission.internal.web.dto.ActiveMissionsResponse;
import com.drc.aidbridge.modules.mission.internal.web.dto.MissionResponse;
import com.drc.aidbridge.modules.shared.enums.MissionStatus;
import com.drc.aidbridge.modules.shared.enums.MissionType;
import com.drc.aidbridge.modules.sos.SosDTO;
import com.drc.aidbridge.modules.sos.SosFacade;
import com.drc.aidbridge.modules.user.UserDTO;
import com.drc.aidbridge.modules.user.UserFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * UseCase: Lấy danh sách missions đang active (cho staff dashboard).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GetActiveMissionsUseCase {

    private final MissionJpaRepository missionRepository;
    private final UserFacade userFacade;
    private final SosFacade sosFacade;
    private final MissionMapper missionMapper;

    public ActiveMissionsResponse execute(MissionType missionType, UUID hubId, boolean includeStats) {
        log.debug("Getting active missions - type: {}, hubId: {}", missionType, hubId);

        // Build page request (lấy tất cả active missions, không phân trang)
        PageRequest pageRequest = PageRequest.of(0, 1000, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Mission> missionsPage;
        if (hubId != null && missionType != null) {
            missionsPage = missionRepository.findActiveByHubIdAndType(hubId, missionType, pageRequest);
        } else if (hubId != null) {
            missionsPage = missionRepository.findActiveByHubId(hubId, pageRequest);
        } else if (missionType != null) {
            missionsPage = missionRepository.findActiveByType(missionType, pageRequest);
        } else {
            missionsPage = missionRepository.findAllActive(pageRequest);
        }

        List<MissionResponse> items = missionsPage.getContent().stream()
                .map(this::toMissionResponse)
                .collect(Collectors.toList());

        ActiveMissionsResponse.ActiveStats stats = null;
        if (includeStats) {
            stats = buildStats(missionsPage.getContent());
        }

        return ActiveMissionsResponse.builder()
                .items(items)
                .stats(stats)
                .cachedAt(Instant.now())
                .build();
    }

    private ActiveMissionsResponse.ActiveStats buildStats(List<Mission> missions) {
        int totalActive = missions.size();
        int rescueCount = (int) missions.stream().filter(m -> m.getMissionType() == MissionType.RESCUE).count();
        int deliveryCount = (int) missions.stream().filter(m -> m.getMissionType() == MissionType.DELIVERY).count();

        // Đếm theo status
        Map<String, Integer> byStatus = new HashMap<>();
        for (MissionStatus status : MissionStatus.values()) {
            int count = (int) missions.stream().filter(m -> m.getStatus() == status).count();
            if (count > 0) {
                byStatus.put(status.name(), count);
            }
        }

        return ActiveMissionsResponse.ActiveStats.builder()
                .totalActive(totalActive)
                .byType(ActiveMissionsResponse.ByTypeStats.builder()
                        .rescue(rescueCount)
                        .delivery(deliveryCount)
                        .build())
                .byStatus(byStatus)
                .build();
    }

    private MissionResponse toMissionResponse(Mission mission) {
        UserDTO volunteer = resolveVolunteer(mission.getVolunteerId());
        SosDTO sos = resolveSos(mission.getSosRequestId());
        return missionMapper.toResponseWithDetails(mission, volunteer, sos);
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
