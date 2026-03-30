package com.drc.aidbridge.modules.mission.internal.mapper;

import com.drc.aidbridge.modules.mission.MissionDTO;
import com.drc.aidbridge.modules.mission.internal.entity.Mission;
import com.drc.aidbridge.modules.mission.internal.web.dto.MissionResponse;
import com.drc.aidbridge.modules.mission.internal.web.dto.MissionTrackingResponse;
import com.drc.aidbridge.modules.sos.SosDTO;
import com.drc.aidbridge.modules.user.UserDTO;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class MissionMapper {

    // Entity → Public module DTO
    public MissionDTO toDTO(Mission mission) {
        return MissionDTO.builder()
                .id(mission.getId())
                .missionType(mission.getMissionType())
                .status(mission.getStatus())
                .sosRequestId(mission.getSosRequestId())
                .aidRequestId(mission.getAidRequestId())
                .helpRequestId(mission.getHelpRequestId())
                .volunteerId(mission.getVolunteerId())
                .hubId(mission.getHubId())
                .victimLat(mission.getVictimLat())
                .victimLng(mission.getVictimLng())
                .createdAt(mission.getCreatedAt())
                .updatedAt(mission.getUpdatedAt())
                .build();
    }

    // Entity → API response (basic, no nested objects)
    public MissionResponse toResponse(Mission mission) {
        return MissionResponse.builder()
                .id(mission.getId())
                .missionType(mission.getMissionType())
                .status(mission.getStatus())
                .sosRequestId(mission.getSosRequestId())
                .aidRequestId(mission.getAidRequestId())
                .helpRequestId(mission.getHelpRequestId())
                .volunteerId(mission.getVolunteerId())
                .hubId(mission.getHubId())
                .victimLat(mission.getVictimLat())
                .victimLng(mission.getVictimLng())
                .qrCodeToken(mission.getQrCodeToken())
                .priorityScore(mission.getPriorityScore())
                .cancellationReason(mission.getCancellationReason())
                .confirmationImageUrl(mission.getConfirmationImageUrl())
                .imageUrl(mission.getImageUrl())
                .comment(mission.getComment())
                .acceptedAt(mission.getAcceptedAt())
                .pickedUpAt(mission.getPickedUpAt())
                .startedAt(mission.getStartedAt())
                .completedAt(mission.getCompletedAt())
                .cancelledAt(mission.getCancelledAt())
                .createdAt(mission.getCreatedAt())
                .updatedAt(mission.getUpdatedAt())
                .build();
    }

    // Entity → API response with nested volunteer and SOS details
    public MissionResponse toResponseWithDetails(Mission mission, UserDTO volunteer) {
        return toResponseWithDetails(mission, volunteer, null);
    }

    public MissionResponse toResponseWithDetails(Mission mission, UserDTO volunteer, SosDTO sos) {
        MissionResponse response = toResponse(mission);

        if (volunteer != null) {
            response.setVolunteer(MissionResponse.VolunteerBrief.builder()
                    .id(java.util.UUID.fromString(volunteer.getId()))
                    .fullName(volunteer.getName())
                    .phoneNumber(volunteer.getPhone())
                    .avatarUrl(volunteer.getAvatarUrl())
                    .build());
        }

                if (sos != null) {
            response.setSosRequest(MissionResponse.SosRequestBrief.builder()
                    .id(sos.getId())
                    .requesterId(sos.getRequesterId())
                    .lat(sos.getLat())
                    .lng(sos.getLng())
                    .address(sos.getAddress())
                    .description(sos.getDescription())
                    .peopleCount(sos.getPeopleCount())
                        .urgencyLevel(sos.getUrgencyLevel() != null ? sos.getUrgencyLevel().name() : null)
                        .status(sos.getStatus() != null ? sos.getStatus().name() : null)
                    .imageUrl(sos.getImageUrl())
                    .createdAt(sos.getCreatedAt())
                    .updatedAt(sos.getUpdatedAt())
                    .build());
        }

        return response;
    }

    // Build tracking response from mission and volunteer data
    public MissionTrackingResponse toTrackingResponse(
            Mission mission, UserDTO volunteer, String destinationAddress,
            Integer etaMinutes, Double distanceKm) {

        MissionTrackingResponse.VolunteerLocation volunteerLocation = null;
        if (volunteer != null) {
            volunteerLocation = MissionTrackingResponse.VolunteerLocation.builder()
                    .id(java.util.UUID.fromString(volunteer.getId()))
                    .fullName(volunteer.getName())
                    .avatarUrl(volunteer.getAvatarUrl())
                    .currentLat(null)
                    .currentLng(null)
                    .lastUpdated(Instant.now())
                    .build();
        }

        MissionTrackingResponse.Destination destination = null;
        if (mission.getVictimLat() != null && mission.getVictimLng() != null) {
            destination = MissionTrackingResponse.Destination.builder()
                    .lat(mission.getVictimLat().doubleValue())
                    .lng(mission.getVictimLng().doubleValue())
                    .build();
        }

        return MissionTrackingResponse.builder()
                .missionId(mission.getId())
                .status(mission.getStatus())
                .volunteer(volunteerLocation)
                .destination(destination)
                .destinationAddress(destinationAddress)
                .etaMinutes(etaMinutes)
                .distanceKm(distanceKm)
                .websocketChannel("missions:" + mission.getId())
                .build();
    }
}
