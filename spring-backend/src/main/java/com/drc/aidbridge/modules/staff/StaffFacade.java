package com.drc.aidbridge.modules.staff;

import com.drc.aidbridge.modules.staff.internal.web.dto.CreateStaffRequest;

import java.util.UUID;

public interface StaffFacade {
    StaffDTO getById(UUID id);
    StaffDTO getByUserId(UUID userId);
    StaffDTO create(CreateStaffRequest request);
}
