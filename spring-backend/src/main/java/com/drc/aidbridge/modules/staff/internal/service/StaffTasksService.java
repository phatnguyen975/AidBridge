package com.drc.aidbridge.modules.staff.internal.service;

import com.drc.aidbridge.modules.donation.internal.entity.Donation;
import com.drc.aidbridge.modules.donation.internal.repository.DonationRepository;
import com.drc.aidbridge.modules.hub.internal.entity.Hub;
import com.drc.aidbridge.modules.hub.internal.entity.HubStaff;
import com.drc.aidbridge.modules.hub.internal.repository.HubRepository;
import com.drc.aidbridge.modules.hub.internal.repository.HubStaffRepository;
import com.drc.aidbridge.modules.mission.internal.entity.Mission;
import com.drc.aidbridge.modules.mission.internal.repository.MissionJpaRepository;
import com.drc.aidbridge.modules.shared.enums.DonationStatus;
import com.drc.aidbridge.modules.shared.enums.MissionStatus;
import com.drc.aidbridge.modules.shared.enums.MissionType;
import com.drc.aidbridge.modules.shared.exception.ResourceNotFoundException;
import com.drc.aidbridge.modules.staff.internal.web.dto.StaffUpcomingDeliveryMissionResponse;
import com.drc.aidbridge.modules.staff.internal.web.dto.StaffUpcomingDonationResponse;
import com.drc.aidbridge.modules.user.UserDTO;
import com.drc.aidbridge.modules.user.UserFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StaffTasksService {

    private static final List<DonationStatus> UPCOMING_DONATION_STATUSES = List.of(DonationStatus.REGISTERED);
    private static final List<MissionStatus> UPCOMING_DELIVERY_STATUSES = List.of(
            MissionStatus.ASSIGNED,
            MissionStatus.PICKING_UP
    );

    private final HubStaffRepository hubStaffRepository;
    private final HubRepository hubRepository;
    private final DonationRepository donationRepository;
    private final MissionJpaRepository missionRepository;
    private final UserFacade userFacade;

    @Transactional(readOnly = true)
    public List<StaffUpcomingDonationResponse> getUpcomingDonations(UUID currentUserId, int page, int limit) {
        Hub staffHub = findActiveStaffHub(currentUserId);
        Pageable pageable = PageRequest.of(resolvePage(page), resolveLimit(limit));

        return donationRepository
                .findByHubIdAndStatusInOrderByCreatedAtAsc(staffHub.getId(), UPCOMING_DONATION_STATUSES, pageable)
                .stream()
                .map(this::toDonationResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<StaffUpcomingDeliveryMissionResponse> getUpcomingDeliveryMissions(UUID currentUserId, int page, int limit) {
        Hub staffHub = findActiveStaffHub(currentUserId);
        Pageable pageable = PageRequest.of(resolvePage(page), resolveLimit(limit));

        return missionRepository
                .findByHubIdAndMissionTypeAndStatusInOrderByCreatedAtAsc(
                        staffHub.getId(),
                        MissionType.DELIVERY,
                        UPCOMING_DELIVERY_STATUSES,
                        pageable
                )
                .stream()
                .map(this::toDeliveryResponse)
                .toList();
    }

    private StaffUpcomingDonationResponse toDonationResponse(Donation donation) {
        UserDTO sponsor = donation.getSponsorId() != null ? userFacade.getUserById(donation.getSponsorId()) : null;
        return new StaffUpcomingDonationResponse(
                donation.getId(),
                safeText(donation.getDonationCode()),
                safeText(sponsor != null ? sponsor.getFullName() : null),
                safeText(sponsor != null ? sponsor.getPhoneNumber() : null)
        );
    }

    private StaffUpcomingDeliveryMissionResponse toDeliveryResponse(Mission mission) {
        UserDTO volunteer = mission.getVolunteerId() != null ? userFacade.getUserById(mission.getVolunteerId()) : null;
        return new StaffUpcomingDeliveryMissionResponse(
                mission.getId(),
                safeText(mission.getCodeName()),
                safeText(volunteer != null ? volunteer.getFullName() : null),
                safeText(volunteer != null ? volunteer.getPhoneNumber() : null)
        );
    }

    private Hub findActiveStaffHub(UUID currentUserId) {
        HubStaff assignment = hubStaffRepository
                .findFirstByUserIdAndIsAvailableTrueAndUnassignedAtIsNullOrderByAssignedAtDesc(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff is not assigned to any active hub"));

        return hubRepository.findById(assignment.getHubId())
                .orElseThrow(() -> new ResourceNotFoundException("Assigned hub not found"));
    }

    private int resolvePage(int page) {
        if (page <= 0) {
            return 0;
        }
        return page - 1;
    }

    private int resolveLimit(int limit) {
        if (limit <= 0) {
            return 20;
        }
        return Math.min(limit, 100);
    }

    private String safeText(String value) {
        return value == null ? "" : value.trim();
    }
}
