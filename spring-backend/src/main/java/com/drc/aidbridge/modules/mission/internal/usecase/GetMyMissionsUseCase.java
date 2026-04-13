package com.drc.aidbridge.modules.mission.internal.usecase;

import com.drc.aidbridge.modules.mission.internal.dispatch.DispatchPolicy;
import com.drc.aidbridge.modules.mission.internal.entity.DispatchAttempt;
import com.drc.aidbridge.modules.mission.internal.entity.Mission;
import com.drc.aidbridge.modules.mission.internal.mapper.MissionMapper;
import com.drc.aidbridge.modules.mission.internal.repository.DispatchAttemptJpaRepository;
import com.drc.aidbridge.modules.mission.internal.repository.MissionJpaRepository;
import com.drc.aidbridge.modules.mission.internal.web.dto.DispatchAttemptResponse;
import com.drc.aidbridge.modules.mission.internal.web.dto.MissionResponse;
import com.drc.aidbridge.modules.mission.internal.web.dto.MyMissionsResponse;
import com.drc.aidbridge.modules.shared.enums.DispatchResponse;
import com.drc.aidbridge.modules.shared.enums.MissionStatus;
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
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class GetMyMissionsUseCase {

    private static final List<MissionStatus> ACTIVE_STATUSES = Arrays.asList(
            MissionStatus.ASSIGNED,
            MissionStatus.PICKING_UP,
            MissionStatus.PICKED_UP,
            MissionStatus.IN_TRANSIT);

    private final MissionJpaRepository missionRepository;
    private final DispatchAttemptJpaRepository dispatchAttemptRepository;
    private final UserFacade userFacade;
    private final SosFacade sosFacade;
    private final MissionMapper missionMapper;

    public MyMissionsResponse execute(UUID volunteerId, boolean includeHistory, int historyPage, int historyLimit) {
        log.debug("Getting missions for volunteer {}", volunteerId);

        List<Mission> activeMissions = missionRepository.findByVolunteerIdAndStatusIn(volunteerId, ACTIVE_STATUSES);
        List<MissionResponse> activeResponses = activeMissions.stream()
                .map(this::toMissionResponse)
                .collect(Collectors.toList());

        List<DispatchAttempt> pendingAttempts = dispatchAttemptRepository
                .findByVolunteerIdAndResponseOrderByCreatedAtDesc(volunteerId, DispatchResponse.PENDING);
        List<MyMissionsResponse.PendingDispatch> pendingDispatches = pendingAttempts.stream()
                .map(this::toPendingDispatch)
                .filter(pendingDispatch -> pendingDispatch != null)
                .collect(Collectors.toList());

        MyMissionsResponse.HistorySection history = null;
        if (includeHistory) {
            PageRequest pageRequest = PageRequest.of(historyPage - 1, historyLimit,
                    Sort.by(Sort.Direction.DESC, "updatedAt"));
            Page<Mission> historyPageResult = missionRepository.findHistoryByVolunteerId(volunteerId, pageRequest);

            List<MissionResponse> historyItems = historyPageResult.getContent().stream()
                    .map(this::toMissionResponse)
                    .collect(Collectors.toList());

            history = MyMissionsResponse.HistorySection.builder()
                    .items(historyItems)
                    .pagination(MyMissionsResponse.PaginationInfo.builder()
                            .page(historyPage)
                            .limit(historyLimit)
                            .total(historyPageResult.getTotalElements())
                            .totalPages(historyPageResult.getTotalPages())
                            .hasNext(historyPageResult.hasNext())
                            .hasPrevious(historyPageResult.hasPrevious())
                            .build())
                    .build();
        }

        return MyMissionsResponse.builder()
                .active(activeResponses)
                .pending(pendingDispatches)
                .history(history)
                .build();
    }

    private MissionResponse toMissionResponse(Mission mission) {
        UserDTO volunteer = resolveVolunteer(mission.getVolunteerId());
        SosDTO sos = resolveSos(mission.getSosRequestId());
        return missionMapper.toResponseWithDetails(mission, volunteer, sos);
    }

    private MyMissionsResponse.PendingDispatch toPendingDispatch(DispatchAttempt attempt) {
        Instant expiresAt = attempt.getCreatedAt().plus(DispatchPolicy.RESPONSE_TIMEOUT);
        if (Instant.now().isAfter(expiresAt)) {
            return null;
        }

        Mission mission = missionRepository.findById(attempt.getMissionId()).orElse(null);
        if (mission == null) {
            return null;
        }

        return MyMissionsResponse.PendingDispatch.builder()
                .dispatchAttempt(toDispatchAttemptResponse(attempt))
                .mission(toMissionResponse(mission))
                .expiresAt(expiresAt)
                .build();
    }

    private DispatchAttemptResponse toDispatchAttemptResponse(DispatchAttempt attempt) {
        return DispatchAttemptResponse.builder()
                .id(attempt.getId())
                .missionId(attempt.getMissionId())
                .volunteerId(attempt.getVolunteerId())
                .dispatchType(attempt.getDispatchType())
                .batchNumber(attempt.getBatchNumber())
                .radiusKm(attempt.getRadiusKm())
                .response(attempt.getResponse())
                .respondedAt(attempt.getRespondedAt())
                .createdAt(attempt.getCreatedAt())
                .build();
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
