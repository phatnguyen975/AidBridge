package com.drc.aidbridge.modules.mission.internal.usecase;

import com.drc.aidbridge.modules.mission.MissionDispatchCreatedEvent;
import com.drc.aidbridge.modules.mission.internal.cache.MissionCacheRedisSchema;
import com.drc.aidbridge.modules.mission.internal.dispatch.DispatchPolicy;
import com.drc.aidbridge.modules.mission.internal.entity.DispatchAttempt;
import com.drc.aidbridge.modules.mission.internal.entity.Mission;
import com.drc.aidbridge.modules.mission.internal.mapper.MissionMapper;
import com.drc.aidbridge.modules.mission.internal.repository.DispatchAttemptJpaRepository;
import com.drc.aidbridge.modules.mission.internal.repository.MissionJpaRepository;
import com.drc.aidbridge.modules.mission.internal.web.dto.MissionResponse;
import com.drc.aidbridge.modules.shared.enums.DispatchResponse;
import com.drc.aidbridge.modules.shared.enums.DispatchType;
import com.drc.aidbridge.modules.shared.enums.MissionStatus;
import com.drc.aidbridge.modules.shared.enums.MissionType;
import com.drc.aidbridge.modules.shared.exception.ResourceNotFoundException;
import com.drc.aidbridge.modules.sos.SosDTO;
import com.drc.aidbridge.modules.sos.SosFacade;
import com.drc.aidbridge.modules.user.UserDTO;
import com.drc.aidbridge.modules.user.UserFacade;
import com.drc.aidbridge.modules.volunteer.VolunteerDTO;
import com.drc.aidbridge.modules.volunteer.VolunteerFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Point;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class DispatchMissionUseCase {

    private final MissionJpaRepository missionRepository;
    private final DispatchAttemptJpaRepository dispatchAttemptRepository;
    private final VolunteerFacade volunteerFacade;
    private final UserFacade userFacade;
    private final SosFacade sosFacade;
    private final MissionMapper missionMapper;
    private final MissionCacheRedisSchema missionCache;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public MissionResponse execute(UUID missionId, List<UUID> preferredVolunteerIds) {
        Mission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> new ResourceNotFoundException("Mission not found: " + missionId));

        // Allow dispatch if PENDING or DISPATCHING (for retries)
        if (mission.getStatus() != MissionStatus.PENDING && mission.getStatus() != MissionStatus.DISPATCHING) {
            log.info("Skipping dispatch for mission {} because status is {}", missionId, mission.getStatus());
            return buildResponse(mission);
        }

        // Increment retry count if this is a subsequent attempt
        if (mission.getLastDispatchAt() != null) {
            mission.setRetryCount(mission.getRetryCount() + 1);
        }
        mission.setLastDispatchAt(Instant.now());

        // Check if max retry limit reached
        if (mission.getRetryCount() >= 5) {
            log.warn("Mission {} reached max retry limit. Setting status to DISPATCH_FAILED", missionId);
            mission.setStatus(MissionStatus.DISPATCH_FAILED);
            Mission savedMission = missionRepository.save(mission);
            return buildResponse(savedMission);
        }

        DispatchPlan plan = buildDispatchPlan(mission, preferredVolunteerIds);
        if (plan.volunteers().isEmpty()) {
            log.info("No eligible volunteers found for mission {} (Attempt {}). Mission remains PENDING", 
                    missionId, mission.getRetryCount() + 1);
            missionRepository.save(mission); // Save lastDispatchAt and retryCount
            return buildResponse(mission);
        }

        int batchNumber = dispatchAttemptRepository.getNextBatchNumber(missionId);
        Instant expiresAt = Instant.now().plus(DispatchPolicy.RESPONSE_TIMEOUT);

        List<MissionDispatchCreatedEvent.DispatchTarget> targets = new ArrayList<>();
        for (VolunteerDTO volunteer : plan.volunteers()) {
            DispatchAttempt attempt = DispatchAttempt.builder()
                    .missionId(missionId)
                    .volunteerId(volunteer.getUserId())
                    .dispatchType(plan.dispatchType())
                    .batchNumber(batchNumber)
                    .radiusKm(plan.radiusKm())
                    .response(DispatchResponse.PENDING)
                    .build();

            DispatchAttempt savedAttempt = dispatchAttemptRepository.save(attempt);
            targets.add(MissionDispatchCreatedEvent.DispatchTarget.builder()
                    .volunteerId(volunteer.getUserId())
                    .dispatchAttemptId(savedAttempt.getId())
                    .batchNumber(batchNumber)
                    .expiresAt(expiresAt)
                    .build());
        }

        mission.setStatus(MissionStatus.DISPATCHING);
        Mission savedMission = missionRepository.save(mission);

        eventPublisher.publishEvent(MissionDispatchCreatedEvent.builder()
                .missionId(savedMission.getId())
                .missionType(savedMission.getMissionType())
                .dispatchType(plan.dispatchType())
                .targets(targets)
                .build());

        MissionResponse response = buildResponse(savedMission);
        missionCache.cacheMission(response);
        return response;
    }

    private DispatchPlan buildDispatchPlan(Mission mission, List<UUID> preferredVolunteerIds) {
        if (preferredVolunteerIds != null && !preferredVolunteerIds.isEmpty()) {
            List<VolunteerDTO> preferredVolunteers = preferredVolunteerIds.stream()
                    .distinct()
                    .map(volunteerFacade::getVolunteerByUserId)
                    .flatMap(Optional::stream)
                    .filter(this::isEligibleVolunteer)
                    .collect(Collectors.toList());

            DispatchType dispatchType = preferredVolunteers.size() > 1
                    ? DispatchType.BROADCAST
                    : DispatchType.SEQUENTIAL;
            BigDecimal radiusKm = calculateMaxDistanceKm(mission.getVictimLocation(), preferredVolunteers);
            return new DispatchPlan(dispatchType, radiusKm, preferredVolunteers);
        }

        return mission.getMissionType() == MissionType.RESCUE
                ? buildRescuePlan(mission)
                : buildDeliveryPlan(mission);
    }

    private DispatchPlan buildRescuePlan(Mission mission) {
        if (mission.getVictimLocation() == null) {
            return DispatchPlan.empty(DispatchType.BROADCAST);
        }

        List<VolunteerDTO> nearby = volunteerFacade.findNearbyVolunteers(
                mission.getVictimLat(),
                mission.getVictimLng(),
                mission.getRetryCount());

        List<VolunteerDTO> selected = nearby.stream()
                .filter(this::isEligibleVolunteer)
                .limit(DispatchPolicy.SOS_BROADCAST_LIMIT) // Top candidates for critical rescue
                .collect(Collectors.toList());

        BigDecimal radiusKm = calculateMaxDistanceKm(mission.getVictimLocation(), selected);
        return new DispatchPlan(DispatchType.BROADCAST, radiusKm, selected);
    }

    private DispatchPlan buildDeliveryPlan(Mission mission) {
        if (mission.getVictimLocation() == null) {
            return DispatchPlan.empty(DispatchType.SEQUENTIAL);
        }

        List<VolunteerDTO> nearby = volunteerFacade.findNearbyVolunteers(
                mission.getVictimLat(),
                mission.getVictimLng(),
                mission.getRetryCount());

        List<VolunteerDTO> selected = nearby.stream()
                .filter(this::isEligibleVolunteer)
                .limit(DispatchPolicy.AID_BATCH_SIZE) // Top candidates for delivery sequential
                .collect(Collectors.toList());

        BigDecimal radiusKm = calculateMaxDistanceKm(mission.getVictimLocation(), selected);
        return new DispatchPlan(DispatchType.SEQUENTIAL, radiusKm, selected);
    }

    private boolean isEligibleVolunteer(VolunteerDTO volunteer) {
        if (volunteer == null) {
            return false;
        }
        return missionRepository.findActiveByVolunteerId(volunteer.getUserId()).isEmpty();
    }

    private BigDecimal calculateMaxDistanceKm(Point missionLocation, Collection<VolunteerDTO> volunteers) {
        if (missionLocation == null || volunteers == null || volunteers.isEmpty()) {
            return null;
        }

        double maxDistanceKm = volunteers.stream()
                .map(VolunteerDTO::getCurrentLocation)
                .filter(location -> location != null)
                .mapToDouble(location -> haversineKm(
                        missionLocation.getY(),
                        missionLocation.getX(),
                        location.getLat().doubleValue(),
                        location.getLng().doubleValue()))
                .max()
                .orElse(0d);

        if (maxDistanceKm <= 0d) {
            return null;
        }

        return BigDecimal.valueOf(maxDistanceKm).setScale(2, RoundingMode.HALF_UP);
    }

    // private BigDecimal toKm(double radiusMeters) {
    //     return BigDecimal.valueOf(radiusMeters / 1000d).setScale(2, RoundingMode.HALF_UP);
    // }

    private double haversineKm(double lat1, double lng1, double lat2, double lng2) {
        double earthRadiusKm = 6371d;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadiusKm * c;
    }

    private MissionResponse buildResponse(Mission mission) {
        UserDTO volunteer = resolveVolunteer(mission.getVolunteerId());
        SosDTO sos = resolveSos(mission.getSosRequestId());
        return missionMapper.toResponseWithDetails(mission, volunteer, sos);
    }

    private UserDTO resolveVolunteer(UUID volunteerId) {
        if (volunteerId == null) {
            return null;
        }

        try {
            return userFacade.getUserById(volunteerId);
        } catch (Exception e) {
            log.warn("Could not resolve volunteer {} while building dispatch response", volunteerId, e);
            return null;
        }
    }

    private SosDTO resolveSos(UUID sosRequestId) {
        if (sosRequestId == null) {
            return null;
        }
        return sosFacade.getSosRequestById(sosRequestId).orElse(null);
    }

    private record DispatchPlan(DispatchType dispatchType, BigDecimal radiusKm, List<VolunteerDTO> volunteers) {
        private static DispatchPlan empty(DispatchType dispatchType) {
            return new DispatchPlan(dispatchType, null, List.of());
        }
    }
}
