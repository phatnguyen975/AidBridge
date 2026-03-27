package com.drc.aidbridge.service;

import com.drc.aidbridge.dto.request.AidRequestItemInputDto;
import com.drc.aidbridge.dto.request.CreateAidRequestDto;
import com.drc.aidbridge.dto.response.AidRequestItemResponseDto;
import com.drc.aidbridge.dto.response.AidRequestResponseDto;
import com.drc.aidbridge.entity.AidRequest;
import com.drc.aidbridge.entity.AidRequestItem;
import com.drc.aidbridge.entity.Mission;
import com.drc.aidbridge.entity.User;
import com.drc.aidbridge.entity.enums.AidStatus;
import com.drc.aidbridge.entity.enums.MissionStatus;
import com.drc.aidbridge.entity.enums.MissionType;
import com.drc.aidbridge.exception.ResourceNotFoundException;
import com.drc.aidbridge.repository.AidRequestItemRepository;
import com.drc.aidbridge.repository.AidRequestRepository;
import com.drc.aidbridge.repository.MissionRepository;
import com.drc.aidbridge.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AidRequestService {

    private final AidRequestRepository aidRequestRepository;
    private final AidRequestItemRepository aidRequestItemRepository;
    private final MissionRepository missionRepository;
    private final UserRepository userRepository;

    @Transactional
    public AidRequestResponseDto createAidRequest(UUID requesterId, CreateAidRequestDto createDto) {
        User requester = userRepository.findById(requesterId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + requesterId));

        int adults = createDto.getAdultsCount() != null ? createDto.getAdultsCount() : 0;
        int elderly = createDto.getElderlyCount() != null ? createDto.getElderlyCount() : 0;
        int children = createDto.getChildrenCount() != null ? createDto.getChildrenCount() : 0;

        if ((adults + elderly + children) <= 0) {
            throw new IllegalArgumentException("Total people count must be greater than 0");
        }

        AidRequest aidRequest = AidRequest.builder()
                .requesterId(requester.getId())
                .status(AidStatus.PENDING)
                .lat(createDto.getLat())
                .lng(createDto.getLng())
                .address(createDto.getAddress())
                .description(createDto.getNotes())
                .numberAdult(adults)
                .numberElderly(elderly)
                .numberChildren(children)
                .build();

        AidRequest saved = aidRequestRepository.save(aidRequest);

        List<AidRequestItem> items = createDto.getItems().stream()
                .map(this::toEntity)
                .peek(i -> i.setAidRequest(saved))
                .collect(Collectors.toList());

        List<AidRequestItem> savedItems = aidRequestItemRepository.saveAll(items);

        saved.setItems(savedItems);

        Mission savedMission = missionRepository.save(Mission.builder()
                .missionType(MissionType.DELIVERY)
                .aidRequestId(saved.getId())
                .status(MissionStatus.PENDING)
                .victimLat(saved.getLat())
                .victimLng(saved.getLng())
                .build());

        return toResponse(saved, savedItems, savedMission);
    }

    private AidRequestItem toEntity(AidRequestItemInputDto dto) {
        return AidRequestItem.builder()
                .itemCategoryId(dto.getItemCategoryId())
                .quantity(dto.getQuantity())
                .description(dto.getDescription())
                .build();
    }

    public AidRequestResponseDto getAidRequest(UUID aidRequestId) {
        AidRequest aidRequest = aidRequestRepository.findById(aidRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("Aid request not found: " + aidRequestId));

        Mission mission = missionRepository.findByAidRequestId(aidRequest.getId()).orElse(null);

        List<AidRequestItem> items = aidRequestItemRepository.findByAidRequestId(aidRequest.getId());

        return toResponse(aidRequest, items, mission);
    }

    private AidRequestItemResponseDto toItemResponse(AidRequestItem item) {
        return AidRequestItemResponseDto.builder()
                .id(item.getId())
                .aidRequestId(item.getAidRequest().getId())
                .itemCategoryId(item.getItemCategoryId())
                .quantity(item.getQuantity())
                .description(item.getDescription())
                .createdAt(item.getCreatedAt())
                .build();
    }

    private AidRequestResponseDto toResponse(AidRequest aidRequest, List<AidRequestItem> items, Mission mission) {
        return AidRequestResponseDto.builder()
                .id(aidRequest.getId())
                .requesterId(aidRequest.getRequesterId())
                .sosRequestId(null)
                .status(aidRequest.getStatus())
                .lat(aidRequest.getLat())
                .lng(aidRequest.getLng())
                .address(aidRequest.getAddress())
                .description(aidRequest.getDescription())
                .numberAdult(aidRequest.getNumberAdult())
                .numberElderly(aidRequest.getNumberElderly())
                .numberChildren(aidRequest.getNumberChildren())
                .urgencyLevel(null)
                .items(items.stream().map(this::toItemResponse).collect(Collectors.toList()))
                .missionId(mission != null ? mission.getId() : null)
                .missionType(mission != null ? mission.getMissionType() : null)
                .missionStatus(mission != null ? mission.getStatus() : null)
                .createdAt(aidRequest.getCreatedAt())
                .updatedAt(aidRequest.getUpdatedAt())
                .build();
    }
}

