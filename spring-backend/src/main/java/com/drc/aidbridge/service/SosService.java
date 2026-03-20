package com.drc.aidbridge.service;

import com.drc.aidbridge.dto.request.CreateSosRequestDto;
import com.drc.aidbridge.dto.request.CreateGuestSosRequestDto;
import com.drc.aidbridge.dto.response.SosRequestResponseDto;
import com.drc.aidbridge.entity.Mission;
import com.drc.aidbridge.entity.SosRequest;
import com.drc.aidbridge.entity.User;
import com.drc.aidbridge.entity.enums.MissionType;
import com.drc.aidbridge.entity.enums.SosStatus;
import com.drc.aidbridge.repository.MissionRepository;
import com.drc.aidbridge.repository.SosRequestRepository;
import com.drc.aidbridge.repository.UserRepository;
import com.drc.aidbridge.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SosService {

    private final SosRequestRepository sosRequestRepository;
    private final MissionRepository missionRepository;
    private final UserRepository userRepository;

    @Transactional
    public SosRequestResponseDto createSosRequest(UUID requesterId, CreateSosRequestDto createDto) {
        User requester = userRepository.findById(requesterId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + requesterId));

        SosRequest sosRequest = SosRequest.builder()
                .requesterId(requester.getId())
                .lat(createDto.getLat())
                .lng(createDto.getLng())
                .address(createDto.getAddress())
                .description(createDto.getDescription())
                .peopleCount(createDto.getPeopleCount() != null ? createDto.getPeopleCount() : 1)
                .urgencyLevel(createDto.getUrgencyLevel() != null ? createDto.getUrgencyLevel() : com.drc.aidbridge.entity.enums.UrgencyLevel.MEDIUM)
                .imageUrl(createDto.getImageUrl())
                .status(SosStatus.PENDING)
                .build();

        SosRequest savedSos = sosRequestRepository.save(sosRequest);

        Mission savedMission = missionRepository.save(
                Mission.builder()
                        .missionType(MissionType.RESCUE)
                        .sosRequest(savedSos)
                        .victimLat(java.math.BigDecimal.valueOf(savedSos.getLat()))
                        .victimLng(java.math.BigDecimal.valueOf(savedSos.getLng()))
                        .status(com.drc.aidbridge.entity.enums.MissionStatus.PENDING)
                        .build());

        return mapToResponse(savedSos, savedMission);
    }

    public SosRequestResponseDto getSosRequest(UUID id) {
        SosRequest sos = sosRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SOS request not found: " + id));
        Mission mission = missionRepository.findBySosRequestId(sos.getId()).orElse(null);
        return mapToResponse(sos, mission);


    }

    public List<SosRequestResponseDto> listSosRequests() {
        List<SosRequest> requests = sosRequestRepository.findAll();
        return requests.stream()
                .map(req -> missionRepository.findBySosRequestId(req.getId())
                        .map(m -> mapToResponse(req, m))
                        .orElseGet(() -> mapToResponse(req, null)))
                .collect(Collectors.toList());
    }

    @Transactional
    public SosRequestResponseDto createGuestSosRequest(CreateGuestSosRequestDto createDto) {
        // Create guest SOS request without requiring authenticated user
        SosRequest sosRequest = SosRequest.builder()
                .requesterId(null)  // Guest user - no requester
                .lat(createDto.getLat())
                .lng(createDto.getLng())
                .address(createDto.getAddress())
                .description(createDto.getDescription())
                .peopleCount(createDto.getPeopleCount() != null ? createDto.getPeopleCount() : 1)
                .urgencyLevel(createDto.getUrgencyLevel() != null ? createDto.getUrgencyLevel() : com.drc.aidbridge.entity.enums.UrgencyLevel.MEDIUM)
                .imageUrl(createDto.getImageUrl())
                .status(SosStatus.PENDING)
                .build();

        SosRequest savedSos = sosRequestRepository.save(sosRequest);

        Mission savedMission = missionRepository.save(
                Mission.builder()
                        .missionType(MissionType.RESCUE)
                        .sosRequest(savedSos)
                        .victimLat(java.math.BigDecimal.valueOf(savedSos.getLat()))
                        .victimLng(java.math.BigDecimal.valueOf(savedSos.getLng()))
                        .status(com.drc.aidbridge.entity.enums.MissionStatus.PENDING)
                        .build());

        return mapToResponse(savedSos, savedMission);
    }

    private SosRequestResponseDto mapToResponse(SosRequest sos, Mission mission) {
        SosRequestResponseDto.SosRequestResponseDtoBuilder builder = SosRequestResponseDto.builder()
                .id(sos.getId())
                .requesterId(sos.getRequesterId())
                .lat(sos.getLat())
                .lng(sos.getLng())
                .address(sos.getAddress())
                .description(sos.getDescription())
                .peopleCount(sos.getPeopleCount())
                .urgencyLevel(sos.getUrgencyLevel())
                .status(sos.getStatus())
                .imageUrl(sos.getImageUrl())
                .createdAt(sos.getCreatedAt())
                .updatedAt(sos.getUpdatedAt());

        if (mission != null) {
            builder.missionId(mission.getId())
                    .missionType(mission.getMissionType())
                    .missionStatus(mission.getStatus());
        }

        return builder.build();
    }
}
