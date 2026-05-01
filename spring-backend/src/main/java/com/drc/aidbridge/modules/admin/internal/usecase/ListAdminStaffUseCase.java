package com.drc.aidbridge.modules.admin.internal.usecase;

import com.drc.aidbridge.modules.admin.internal.web.dto.AdminStaffResponse;
import com.drc.aidbridge.modules.hub.HubDTO;
import com.drc.aidbridge.modules.hub.HubFacade;
import com.drc.aidbridge.modules.hub.HubStaffDTO;
import com.drc.aidbridge.modules.shared.enums.UserRole;
import com.drc.aidbridge.modules.user.UserDTO;
import com.drc.aidbridge.modules.user.UserFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ListAdminStaffUseCase {

    private final UserFacade userFacade;
    private final HubFacade hubFacade;

    @Transactional(readOnly = true)
    public List<AdminStaffResponse> execute() {
        List<UserDTO> staffUsers = userFacade.findUsersByRole(UserRole.STAFF);
        if (staffUsers.isEmpty()) {
            return List.of();
        }

        List<UUID> userIds = staffUsers.stream()
                .map(UserDTO::getId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        List<HubStaffDTO> assignments = userIds.isEmpty()
                ? List.of()
                : hubFacade.findActiveAssignmentsByUserIds(userIds);
        
        Map<UUID, HubStaffDTO> assignmentByUserId = assignments.stream()
                .filter(assignment -> assignment.getUserId() != null)
                .collect(Collectors.toMap(HubStaffDTO::getUserId, Function.identity(), (left, right) -> left));

        Set<UUID> hubIds = assignments.stream()
                .map(HubStaffDTO::getHubId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        
        Map<UUID, HubDTO> hubById = hubIds.stream()
                .map(hubFacade::getById)
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(HubDTO::getId, Function.identity(), (left, right) -> left));

        List<AdminStaffResponse> responses = new ArrayList<>();
        for (UserDTO user : staffUsers) {
            if (user == null || user.getId() == null) {
                continue;
            }

            HubStaffDTO assignment = assignmentByUserId.get(user.getId());
            HubDTO hub = assignment != null ? hubById.get(assignment.getHubId()) : null;

            responses.add(new AdminStaffResponse(
                    user.getId(),
                    user.getId(),
                    user.getFullName(),
                    user.getEmail(),
                    user.getPhoneNumber(),
                    assignment != null ? assignment.getHubId() : null,
                    hub != null ? hub.getName() : ""
            ));
        }
        return responses;
    }
}
