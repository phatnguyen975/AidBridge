
package com.drc.aidbridge.modules.staff.internal.usecase;

import com.drc.aidbridge.modules.shared.enums.UserRole;
import com.drc.aidbridge.modules.shared.exception.ResourceNotFoundException;
import com.drc.aidbridge.modules.staff.StaffDTO;
import com.drc.aidbridge.modules.staff.internal.mapper.StaffMapper;
import com.drc.aidbridge.modules.staff.internal.web.dto.CreateStaffRequest;
import com.drc.aidbridge.modules.user.internal.entity.User;
import com.drc.aidbridge.modules.user.internal.repository.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class CreateStaffUseCase {

    private final UserJpaRepository userRepository;
    private final StaffMapper staffMapper;

    @Transactional
    public StaffDTO execute(CreateStaffRequest request) {
        if (request == null) throw new IllegalArgumentException("request is null");
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + request.getUserId()));
        if (user.getRole() == UserRole.STAFF) {
            throw new IllegalStateException("Staff already exists for user");
        }
        user.setRole(UserRole.STAFF);
        User saved = userRepository.save(user);
        return staffMapper.toDTO(saved);
    }
}
