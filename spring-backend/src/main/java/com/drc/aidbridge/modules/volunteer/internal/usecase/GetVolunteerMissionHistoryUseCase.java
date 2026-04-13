package com.drc.aidbridge.modules.volunteer.internal.usecase;

import com.drc.aidbridge.modules.mission.internal.repository.MissionHistoryProjection;
import com.drc.aidbridge.modules.mission.internal.repository.MissionJpaRepository;
import com.drc.aidbridge.modules.shared.enums.MissionType;
import com.drc.aidbridge.modules.shared.exception.ResourceNotFoundException;
import com.drc.aidbridge.modules.volunteer.internal.repository.VolunteerJpaRepository;
import com.drc.aidbridge.modules.volunteer.internal.web.dto.VolunteerMissionHistoryResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Use case for retrieving mission history of the authenticated volunteer.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GetVolunteerMissionHistoryUseCase {

    private final VolunteerJpaRepository volunteerRepository;
    private final MissionJpaRepository missionRepository;

    /**
     * Returns paginated mission history including completed and cancelled missions.
     *
     * @param userId authenticated user id of volunteer
     * @param page 1-based page index
     * @param limit page size
     * @return mission history response
     */
    @Transactional(readOnly = true)
    public VolunteerMissionHistoryResponse execute(UUID userId, int page, int limit) {
        validatePagination(page, limit);

        if (!volunteerRepository.existsByUserId(userId)) {
            throw new ResourceNotFoundException("Volunteer profile not found");
        }

        PageRequest pageRequest = PageRequest.of(page - 1, limit);
        Page<MissionHistoryProjection> historyPage = missionRepository.findHistoryProjectionByVolunteerId(userId, pageRequest);

        List<VolunteerMissionHistoryResponse.MissionHistoryItem> items = historyPage.getContent().stream()
            .map(this::toMissionResponse)
                .toList();

        log.debug("Retrieved {} history missions for volunteer {}", items.size(), userId);

        return VolunteerMissionHistoryResponse.builder()
                .items(items)
                .pagination(VolunteerMissionHistoryResponse.PaginationInfo.builder()
                        .page(page)
                        .limit(limit)
                        .total(historyPage.getTotalElements())
                        .totalPages(historyPage.getTotalPages())
                        .hasNext(historyPage.hasNext())
                        .hasPrevious(historyPage.hasPrevious())
                        .build())
                .build();
    }

    private void validatePagination(int page, int limit) {
        if (page < 1) {
            throw new IllegalArgumentException("page must be greater than or equal to 1");
        }
        if (limit < 1) {
            throw new IllegalArgumentException("limit must be greater than or equal to 1");
        }
    }

    private VolunteerMissionHistoryResponse.MissionHistoryItem toMissionResponse(MissionHistoryProjection row) {
        return VolunteerMissionHistoryResponse.MissionHistoryItem.builder()
                .completedAt(row.getCompletedAt())
                .missionType(parseMissionType(row.getMissionType()))
                .address(row.getAddress())
                .build();
    }

    private MissionType parseMissionType(String missionType) {
        if (missionType == null || missionType.isBlank()) {
            return null;
        }
        return MissionType.valueOf(missionType);
    }

}
