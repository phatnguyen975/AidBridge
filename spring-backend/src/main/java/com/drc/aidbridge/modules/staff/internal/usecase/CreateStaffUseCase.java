
package com.drc.aidbridge.modules.staff.internal.usecase;

import com.drc.aidbridge.modules.staff.StaffDTO;
import com.drc.aidbridge.modules.staff.internal.entity.Staff;
import com.drc.aidbridge.modules.staff.internal.mapper.StaffMapper;
import com.drc.aidbridge.modules.staff.internal.repository.StaffRepository;
import com.drc.aidbridge.modules.staff.internal.web.dto.CreateStaffRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class CreateStaffUseCase {

    private final StaffRepository staffRepository;
    private final StaffMapper staffMapper;

    @Transactional
    public StaffDTO execute(CreateStaffRequest request) {
        if (request == null) throw new IllegalArgumentException("request is null");
        staffRepository.findByUserId(request.getUserId()).ifPresent(s -> { throw new IllegalStateException("Staff already exists for user"); });
        Staff entity = Staff.builder()
                .userId(request.getUserId())
                .startDate(request.getStartDate())
                .build();
        Staff saved = staffRepository.save(entity);
        return staffMapper.toDTO(saved);
    }
}
