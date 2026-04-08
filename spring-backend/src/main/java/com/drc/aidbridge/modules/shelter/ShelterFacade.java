package com.drc.aidbridge.modules.shelter;

import com.drc.aidbridge.modules.shelter.internal.web.dto.CreateShelterRequest;

import java.util.List;
import java.util.UUID;

public interface ShelterFacade {
    ShelterDTO getById(UUID id);
    ShelterDTO create(CreateShelterRequest request);
    List<ShelterDTO> findActive();
}
