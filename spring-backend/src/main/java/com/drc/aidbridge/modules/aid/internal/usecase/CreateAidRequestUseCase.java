package com.drc.aidbridge.modules.aid.internal.usecase;

import com.drc.aidbridge.entity.enums.AidStatus;
import com.drc.aidbridge.entity.enums.MissionStatus;
import com.drc.aidbridge.entity.enums.MissionType;
import com.drc.aidbridge.modules.aid.internal.entity.AidRequest;
import com.drc.aidbridge.modules.aid.internal.entity.AidRequestItem;
import com.drc.aidbridge.modules.aid.internal.mapper.AidMapper;
import com.drc.aidbridge.modules.aid.internal.repository.AidRequestItemJpaRepository;
import com.drc.aidbridge.modules.aid.internal.repository.AidRequestJpaRepository;
import com.drc.aidbridge.modules.aid.internal.web.dto.AidRequestResponse;
import com.drc.aidbridge.modules.aid.internal.web.dto.CreateAidRequest;
import com.drc.aidbridge.modules.user.UserFacade;
import com.drc.aidbridge.entity.Mission;
import com.drc.aidbridge.repository.MissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class CreateAidRequestUseCase {

    private final AidRequestJpaRepository aidRequestRepository;
    private final AidRequestItemJpaRepository aidRequestItemRepository;
    private final MissionRepository missionRepository;
    private final UserFacade userFacade;
    private final AidMapper aidMapper;

    @Transactional
    public AidRequestResponse execute(UUID requesterId, CreateAidRequest request) {
        // Validate requester exists via facade
        userFacade.getUserById(requesterId);

        int adults = request.getAdultsCount() != null ? request.getAdultsCount() : 0;
        int elderly = request.getElderlyCount() != null ? request.getElderlyCount() : 0;
        int children = request.getChildrenCount() != null ? request.getChildrenCount() : 0;

        if ((adults + elderly + children) <= 0) {
            throw new IllegalArgumentException("Total people count must be greater than 0");
        }

        AidRequest aidRequest = AidRequest.builder()
                .requesterId(requesterId)
                .status(AidStatus.PENDING)
                .lat(request.getLat())
                .lng(request.getLng())
                .address(request.getAddress())
                .description(request.getNotes())
                .numberAdult(adults)
                .numberElderly(elderly)
                .numberChildren(children)
                .build();

        AidRequest saved = aidRequestRepository.save(aidRequest);

        List<AidRequestItem> items = request.getItems().stream()
                .map(aidMapper::toItemEntity)
                .peek(i -> i.setAidRequest(saved))
                .collect(Collectors.toList());

        List<AidRequestItem> savedItems = aidRequestItemRepository.saveAll(items);
        saved.setItems(savedItems);

        // Create associated mission
        Mission savedMission = missionRepository.save(Mission.builder()
                .missionType(MissionType.DELIVERY)
                .aidRequestId(saved.getId())
                .status(MissionStatus.PENDING)
                .victimLat(saved.getLat())
                .victimLng(saved.getLng())
                .build());

        return aidMapper.toResponse(saved, savedItems, savedMission);
    }
}
