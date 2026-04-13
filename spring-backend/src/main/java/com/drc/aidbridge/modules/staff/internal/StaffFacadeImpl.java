package com.drc.aidbridge.modules.staff.internal;

import com.drc.aidbridge.modules.staff.StaffDTO;
import com.drc.aidbridge.modules.staff.StaffFacade;
import com.drc.aidbridge.modules.staff.internal.usecase.CreateStaffUseCase;
import com.drc.aidbridge.modules.staff.internal.usecase.GetStaffByIdUseCase;
import com.drc.aidbridge.modules.staff.internal.usecase.GetStaffByUserIdUseCase;
import com.drc.aidbridge.modules.staff.internal.web.dto.CreateStaffRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StaffFacadeImpl implements StaffFacade {

    private final GetStaffByIdUseCase getStaffByIdUseCase;
    private final GetStaffByUserIdUseCase getStaffByUserIdUseCase;
    private final CreateStaffUseCase createStaffUseCase;

    @Override
    public StaffDTO getById(UUID id) {
        return getStaffByIdUseCase.execute(id);
    }

    @Override
    public StaffDTO getByUserId(UUID userId) {
        return getStaffByUserIdUseCase.execute(userId);
    }

    @Override
    public StaffDTO create(CreateStaffRequest request) {
        return createStaffUseCase.execute(request);
    }
}
