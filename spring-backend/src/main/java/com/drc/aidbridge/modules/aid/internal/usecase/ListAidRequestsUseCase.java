package com.drc.aidbridge.modules.aid.internal.usecase;

import com.drc.aidbridge.modules.shared.dto.PaginatedResponseDto;
import com.drc.aidbridge.modules.shared.dto.PaginationDto;
import com.drc.aidbridge.modules.aid.internal.entity.AidRequest;
import com.drc.aidbridge.modules.aid.internal.entity.AidRequestItem;
import com.drc.aidbridge.modules.aid.internal.mapper.AidMapper;
import com.drc.aidbridge.modules.aid.internal.repository.AidRequestItemJpaRepository;
import com.drc.aidbridge.modules.aid.internal.repository.AidRequestJpaRepository;
import com.drc.aidbridge.modules.aid.internal.web.dto.AidRequestResponse;
import com.drc.aidbridge.modules.mission.MissionDTO;
import com.drc.aidbridge.modules.mission.MissionFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ListAidRequestsUseCase {

    private final AidRequestJpaRepository aidRequestRepository;
    private final AidRequestItemJpaRepository aidRequestItemRepository;
    private final MissionFacade missionFacade;
    private final AidMapper aidMapper;

    public PaginatedResponseDto<AidRequestResponse> execute(int page, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 100));
        int safePage = Math.max(page, 0);
        Pageable pageable = PageRequest.of(safePage, safeLimit);

        Page<AidRequest> paged = aidRequestRepository.findAll(pageable);

        List<AidRequestResponse> items = paged.getContent().stream()
                .map(aidRequest -> {
                    MissionDTO mission = missionFacade.findMissionByAidRequestId(aidRequest.getId()).orElse(null);
                    List<AidRequestItem> aidRequestItems = aidRequestItemRepository.findByAidRequestId(aidRequest.getId());
                    return aidMapper.toResponse(aidRequest, aidRequestItems, mission);
                })
                .collect(Collectors.toList());

        PaginationDto pagination = PaginationDto.builder()
                .page(safePage)
                .limit(safeLimit)
                .total(paged.getTotalElements())
                .totalPages(paged.getTotalPages())
                .hasNext(paged.hasNext())
                .build();

        return PaginatedResponseDto.<AidRequestResponse>builder()
                .items(items)
                .pagination(pagination)
                .build();
    }
}
