package com.drc.aidbridge.modules.mission.internal.usecase;

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

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * UseCase: Lấy danh sách missions của volunteer hiện tại.
 * Bao gồm: active, pending dispatch, history.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GetMyMissionsUseCase {

    private final MissionJpaRepository missionRepository;
    private final DispatchAttemptJpaRepository dispatchAttemptRepository;
    private final UserFacade userFacade;
    private final SosFacade sosFacade;
    private final MissionMapper missionMapper;

    // Dispatch timeout: 5 phút
    private static final Duration DISPATCH_TIMEOUT = Duration.ofMinutes(5);

    // Status được coi là "active"
    private static final List<MissionStatus> ACTIVE_STATUSES = Arrays.asList(
            MissionStatus.ASSIGNED,
            MissionStatus.PICKING_UP,
            MissionStatus.PICKED_UP,
            MissionStatus.IN_TRANSIT);

    public MyMissionsResponse execute(UUID volunteerId, boolean includeHistory, int historyPage, int historyLimit) {
        log.debug("Getting missions for volunteer {}", volunteerId);

        // 1. Lấy active missions
        List<Mission> activeMissions = missionRepository.findByVolunteerIdAndStatusIn(volunteerId, ACTIVE_STATUSES);
        List<MissionResponse> activeResponses = activeMissions.stream()
                .map(this::toMissionResponse)
                .collect(Collectors.toList());

        // 2. Lấy pending dispatch requests
        List<DispatchAttempt> pendingAttempts = dispatchAttemptRepository
                .findByVolunteerIdAndResponseOrderByCreatedAtDesc(volunteerId, DispatchResponse.PENDING);
        List<MyMissionsResponse.PendingDispatch> pendingDispatches = pendingAttempts.stream()
                .map(this::toPendingDispatch)
                .filter(pd -> pd != null) // Lọc bỏ các dispatch đã hết hạn
                .collect(Collectors.toList());

        // 3. Lấy history nếu yêu cầu
        MyMissionsResponse.HistorySection history = null;
        if (includeHistory) {
            PageRequest pageRequest = PageRequest.of(historyPage - 1, historyLimit,
                    Sort.by(Sort.Direction.DESC, "updatedAt"));
            Page<Mission> historyPage1 = missionRepository.findHistoryByVolunteerId(volunteerId, pageRequest);

            List<MissionResponse> historyItems = historyPage1.getContent().stream()
                    .map(this::toMissionResponse)
                    .collect(Collectors.toList());

            history = MyMissionsResponse.HistorySection.builder()
                    .items(historyItems)
                    .pagination(MyMissionsResponse.PaginationInfo.builder()
                            .page(historyPage)
                            .limit(historyLimit)
                            .total(historyPage1.getTotalElements())
                            .totalPages(historyPage1.getTotalPages())
                            .hasNext(historyPage1.hasNext())
                            .hasPrevious(historyPage1.hasPrevious())
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
        Instant expiresAt = attempt.getCreatedAt().plus(DISPATCH_TIMEOUT);

        // Kiểm tra đã hết hạn chưa
        if (Instant.now().isAfter(expiresAt)) {
            return null;
        }

        Mission mission = missionRepository.findById(attempt.getMissionId()).orElse(null);
        if (mission == null) {
            return null;
        }

        MissionResponse missionResponse = toMissionResponse(mission);
        DispatchAttemptResponse attemptResponse = toDispatchAttemptResponse(attempt);

        return MyMissionsResponse.PendingDispatch.builder()
                .dispatchAttempt(attemptResponse)
                .mission(missionResponse)
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
