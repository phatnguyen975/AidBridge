package com.drc.aidbridge.modules.aid.internal;

import com.drc.aidbridge.exception.ResourceNotFoundException;
import com.drc.aidbridge.modules.aid.AidFacade;
import com.drc.aidbridge.modules.aid.AidRequestDTO;
import com.drc.aidbridge.modules.aid.internal.mapper.AidMapper;
import com.drc.aidbridge.modules.aid.internal.repository.AidRequestJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AidFacadeImpl implements AidFacade {

    private final AidRequestJpaRepository aidRequestRepository;
    private final AidMapper aidMapper;

    @Override
    public AidRequestDTO getAidRequestById(UUID aidRequestId) {
        return aidRequestRepository.findById(aidRequestId)
                .map(aidMapper::toDTO)
                .orElseThrow(() -> new ResourceNotFoundException("Aid request not found: " + aidRequestId));
    }

    @Override
    public boolean existsById(UUID aidRequestId) {
        return aidRequestRepository.existsById(aidRequestId);
    }
}
