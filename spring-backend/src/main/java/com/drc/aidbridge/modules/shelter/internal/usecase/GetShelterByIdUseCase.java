package com.drc.aidbridge.modules.shelter.internal.usecase;

import com.drc.aidbridge.modules.shelter.ShelterDTO;
import com.drc.aidbridge.modules.shelter.internal.mapper.ShelterMapper;
import com.drc.aidbridge.modules.shelter.internal.repository.ShelterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class GetShelterByIdUseCase {

    private final ShelterRepository shelterRepository;
    private final ShelterMapper shelterMapper;

    public ShelterDTO execute(UUID id) {
        return shelterRepository.findById(id).map(shelterMapper::toDTO).orElse(null);
    }
}
