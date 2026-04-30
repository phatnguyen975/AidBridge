
package com.drc.aidbridge.modules.staff.internal.usecase;

import com.drc.aidbridge.modules.shared.enums.UserRole;
import com.drc.aidbridge.modules.staff.StaffDTO;
import com.drc.aidbridge.modules.staff.internal.mapper.StaffMapper;
import com.drc.aidbridge.modules.user.internal.repository.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class GetStaffByUserIdUseCase {

    private final UserJpaRepository userRepository;
    private final StaffMapper staffMapper;

    public StaffDTO execute(UUID userId) {
        return userRepository.findById(userId)
                .filter(user -> user.getRole() == UserRole.STAFF)
                .map(staffMapper::toDTO)
                .orElse(null);
    }
}
