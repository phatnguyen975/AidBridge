package com.drc.aidbridge.modules.mission.internal.usecase;

import com.drc.aidbridge.modules.mission.internal.entity.DispatchAttempt;
import com.drc.aidbridge.modules.mission.internal.repository.DispatchAttemptJpaRepository;
import com.drc.aidbridge.modules.mission.internal.repository.MissionJpaRepository;
import com.drc.aidbridge.modules.mission.internal.web.dto.DispatchAttemptResponse;
import com.drc.aidbridge.modules.mission.internal.web.dto.DispatchAttemptsListResponse;
import com.drc.aidbridge.modules.shared.exception.ResourceNotFoundException;
import com.drc.aidbridge.modules.user.UserDTO;
import com.drc.aidbridge.modules.user.UserFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * UseCase: Lấy lịch sử dispatch attempts của mission.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GetDispatchAttemptsUseCase {

    private final MissionJpaRepository missionRepository;
    private final DispatchAttemptJpaRepository dispatchAttemptRepository;
    private final UserFacade userFacade;

    public DispatchAttemptsListResponse execute(UUID missionId, Pageable pageable) {
        log.debug("Getting dispatch attempts for mission {}", missionId);

        // Validate mission exists
        if (!missionRepository.existsById(missionId)) {
            throw new ResourceNotFoundException("Mission not found: " + missionId);
        }

        Page<DispatchAttempt> page = dispatchAttemptRepository.findByMissionIdOrderByCreatedAtDesc(missionId, pageable);

        // Batch load volunteer info
        List<UUID> volunteerIds = page.getContent().stream()
                .map(DispatchAttempt::getVolunteerId)
                .distinct()
                .toList();

        Map<UUID, UserDTO> volunteerMap = volunteerIds.stream()
                .collect(Collectors.toMap(
                        Function.identity(),
                        id -> {
                            try {
                                return userFacade.getUserById(id);
                            } catch (Exception e) {
                                log.warn("Could not resolve volunteer {}: {}", id, e.getMessage());
                                return null;
                            }
                        }));

        List<DispatchAttemptResponse> items = page.getContent().stream()
                .map(attempt -> toResponse(attempt, volunteerMap.get(attempt.getVolunteerId())))
                .toList();

        return DispatchAttemptsListResponse.builder()
                .items(items)
                .pagination(DispatchAttemptsListResponse.PaginationInfo.builder()
                        .page(page.getNumber() + 1)
                        .limit(page.getSize())
                        .total(page.getTotalElements())
                        .totalPages(page.getTotalPages())
                        .hasNext(page.hasNext())
                        .hasPrevious(page.hasPrevious())
                        .build())
                .build();
    }

    private DispatchAttemptResponse toResponse(DispatchAttempt attempt, UserDTO volunteer) {
        DispatchAttemptResponse.VolunteerBrief volunteerBrief = null;
        if (volunteer != null) {
            volunteerBrief = DispatchAttemptResponse.VolunteerBrief.builder()
                    .id(UUID.fromString(volunteer.getId()))
                    .fullName(volunteer.getName())
                    .phoneNumber(volunteer.getPhone())
                    .avatarUrl(volunteer.getAvatarUrl())
                    .build();
        }

        return DispatchAttemptResponse.builder()
                .id(attempt.getId())
                .missionId(attempt.getMissionId())
                .volunteerId(attempt.getVolunteerId())
                .volunteer(volunteerBrief)
                .dispatchType(attempt.getDispatchType())
                .batchNumber(attempt.getBatchNumber())
                .radiusKm(attempt.getRadiusKm())
                .response(attempt.getResponse())
                .respondedAt(attempt.getRespondedAt())
                .createdAt(attempt.getCreatedAt())
                .build();
    }
}
