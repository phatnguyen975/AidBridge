package com.drc.aidbridge.modules.volunteer.internal.usecase;

import com.drc.aidbridge.modules.mission.MissionFacade;
import com.drc.aidbridge.modules.mission.MissionHistoryFullDTO;
import com.drc.aidbridge.modules.shared.exception.ResourceNotFoundException;
import com.drc.aidbridge.modules.volunteer.internal.repository.VolunteerJpaRepository;
import com.drc.aidbridge.modules.volunteer.internal.web.dto.VolunteerMissionHistoryFullResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class GetVolunteerMissionHistoryFullUseCase {

    private final VolunteerJpaRepository volunteerRepository;
    private final MissionFacade missionFacade;

    @Transactional(readOnly = true)
    public VolunteerMissionHistoryFullResponse execute(UUID userId, int page, int limit) {
        validatePagination(page, limit);

        if (!volunteerRepository.existsByUserId(userId)) {
            throw new ResourceNotFoundException("Volunteer profile not found");
        }

        PageRequest pageRequest = PageRequest.of(page - 1, limit);
        Page<MissionHistoryFullDTO> historyPage = missionFacade.findFullHistoryByVolunteerId(userId, pageRequest);

        log.debug("Retrieved {} full history missions for volunteer {}", historyPage.getNumberOfElements(), userId);

        return VolunteerMissionHistoryFullResponse.builder()
                .items(historyPage.getContent())
                .pagination(VolunteerMissionHistoryFullResponse.PaginationInfo.builder()
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
}
