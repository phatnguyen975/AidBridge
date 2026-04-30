package com.drc.aidbridge.modules.admin.internal.usecase;

import com.drc.aidbridge.modules.admin.internal.web.dto.AdminStaffResponse;
import com.drc.aidbridge.modules.hub.internal.entity.Hub;
import com.drc.aidbridge.modules.hub.internal.entity.HubStaff;
import com.drc.aidbridge.modules.hub.internal.repository.HubRepository;
import com.drc.aidbridge.modules.hub.internal.repository.HubStaffRepository;
import com.drc.aidbridge.modules.shared.enums.UserRole;
import com.drc.aidbridge.modules.user.internal.entity.User;
import com.drc.aidbridge.modules.user.internal.repository.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
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

    private final UserJpaRepository userRepository;
    private final HubStaffRepository hubStaffRepository;
    private final HubRepository hubRepository;

    @Transactional(readOnly = true)
    public List<AdminStaffResponse> execute() {
        List<User> staffUsers = userRepository.findByRole(UserRole.STAFF);
        if (staffUsers.isEmpty()) {
            return List.of();
        }

        List<UUID> userIds = staffUsers.stream()
                .map(User::getId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        List<HubStaff> assignments = userIds.isEmpty()
                ? List.of()
                : hubStaffRepository.findByUserIdInAndUnassignedAtIsNull(userIds);
        Map<UUID, HubStaff> assignmentByUserId = assignments.stream()
                .filter(assignment -> assignment.getUserId() != null)
                .collect(Collectors.toMap(HubStaff::getUserId, Function.identity(), (left, right) -> left));

        Set<UUID> hubIds = assignments.stream()
                .map(HubStaff::getHubId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<UUID, Hub> hubById = hubRepository.findAllById(hubIds).stream()
                .collect(Collectors.toMap(Hub::getId, Function.identity(), (left, right) -> left));

        List<AdminStaffResponse> responses = new ArrayList<>();
        for (User user : staffUsers) {
            if (user == null || user.getId() == null) {
                continue;
            }

            HubStaff assignment = assignmentByUserId.get(user.getId());
            Hub hub = assignment != null ? hubById.get(assignment.getHubId()) : null;

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
