
package com.drc.aidbridge.modules.staff.internal.usecase;

import com.drc.aidbridge.modules.staff.StaffDTO;
import com.drc.aidbridge.modules.staff.internal.mapper.StaffMapper;
import com.drc.aidbridge.modules.staff.internal.repository.StaffRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class GetStaffByIdUseCase {

    private final StaffRepository staffRepository;
    private final StaffMapper staffMapper;

    public StaffDTO execute(UUID id) {
        return staffRepository.findById(id).map(staffMapper::toDTO).orElse(null);
    }
}
