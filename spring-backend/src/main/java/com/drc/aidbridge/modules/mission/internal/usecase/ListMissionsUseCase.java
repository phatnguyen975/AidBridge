package com.drc.aidbridge.modules.mission.internal.usecase;

import com.drc.aidbridge.entity.enums.MissionStatus;
import com.drc.aidbridge.entity.enums.MissionType;
import com.drc.aidbridge.modules.mission.internal.entity.Mission;
import com.drc.aidbridge.modules.mission.internal.mapper.MissionMapper;
import com.drc.aidbridge.modules.mission.internal.repository.MissionJpaRepository;
import com.drc.aidbridge.modules.mission.internal.web.dto.MissionListResponse;
import com.drc.aidbridge.modules.mission.internal.web.dto.MissionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ListMissionsUseCase {

    private final MissionJpaRepository missionRepository;
    private final MissionMapper missionMapper;

    // Dynamic filter routing based on which params are non-null
    public MissionListResponse execute(
            MissionType missionType, MissionStatus status, UUID volunteerId, int page, int limit) {

        Pageable pageable = PageRequest.of(page - 1, limit, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Mission> missionPage;
        if (missionType != null && status != null && volunteerId != null) {
            missionPage = missionRepository.findByMissionTypeAndStatusAndVolunteerId(missionType, status, volunteerId, pageable);
        } else if (missionType != null && status != null) {
            missionPage = missionRepository.findByMissionTypeAndStatus(missionType, status, pageable);
        } else if (missionType != null && volunteerId != null) {
            missionPage = missionRepository.findByMissionTypeAndVolunteerId(missionType, volunteerId, pageable);
        } else if (status != null && volunteerId != null) {
            missionPage = missionRepository.findByStatusAndVolunteerId(status, volunteerId, pageable);
        } else if (missionType != null) {
            missionPage = missionRepository.findByMissionType(missionType, pageable);
        } else if (status != null) {
            missionPage = missionRepository.findByStatus(status, pageable);
        } else if (volunteerId != null) {
            missionPage = missionRepository.findByVolunteerId(volunteerId, pageable);
        } else {
            missionPage = missionRepository.findAll(pageable);
        }

        List<MissionResponse> items = missionPage.getContent().stream()
                .map(missionMapper::toResponse)
                .collect(Collectors.toList());

        MissionListResponse.PaginationInfo pagination = MissionListResponse.PaginationInfo.builder()
                .page(page)
                .limit(limit)
                .total(missionPage.getTotalElements())
                .totalPages(missionPage.getTotalPages())
                .hasNext(missionPage.hasNext())
                .hasPrevious(missionPage.hasPrevious())
                .build();

        return MissionListResponse.builder()
                .items(items)
                .pagination(pagination)
                .build();
    }
}
