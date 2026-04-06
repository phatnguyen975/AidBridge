package com.drc.aidbridge.modules.shelter.internal;

import com.drc.aidbridge.modules.shelter.ShelterDTO;
import com.drc.aidbridge.modules.shelter.ShelterFacade;
import com.drc.aidbridge.modules.shelter.internal.entity.Shelter;
import com.drc.aidbridge.modules.shelter.internal.mapper.ShelterMapper;
import com.drc.aidbridge.modules.shelter.internal.repository.ShelterRepository;
import com.drc.aidbridge.modules.shelter.internal.web.dto.CreateShelterRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ShelterFacadeImpl implements ShelterFacade {

    private final ShelterRepository shelterRepository;
    private final ShelterMapper shelterMapper;

    @Override
    public ShelterDTO getById(UUID id) {
        return shelterRepository.findById(id).map(shelterMapper::toDTO).orElse(null);
    }

    @Override
    @Transactional
    public ShelterDTO create(CreateShelterRequest request) {
        if (request == null) throw new IllegalArgumentException("request is null");
        Shelter entity = shelterMapper.toEntity(request);
        Shelter saved = shelterRepository.save(entity);
        return shelterMapper.toDTO(saved);
    }

    @Override
    public List<ShelterDTO> findActive() {
        return shelterRepository.findByIsActiveTrue().stream().map(shelterMapper::toDTO).collect(Collectors.toList());
    }
}
