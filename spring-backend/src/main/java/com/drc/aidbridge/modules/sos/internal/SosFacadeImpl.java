package com.drc.aidbridge.modules.sos.internal;

import com.drc.aidbridge.modules.shared.enums.SosStatus;
import com.drc.aidbridge.modules.shared.exception.ResourceNotFoundException;
import com.drc.aidbridge.modules.sos.SosDTO;
import com.drc.aidbridge.modules.sos.SosFacade;
import com.drc.aidbridge.modules.sos.internal.mapper.SosMapper;
import com.drc.aidbridge.modules.sos.internal.repository.SosJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SosFacadeImpl implements SosFacade {

    private final SosJpaRepository sosRequestRepository;
    private final SosMapper sosMapper;

    @Override
    public Optional<SosDTO> getSosRequestById(UUID id) {
        return sosRequestRepository.findById(id).map(sosMapper::toDTO);
    }

    @Override
    public void updateStatus(UUID id, SosStatus status) {
        sosRequestRepository.findById(id)
                .ifPresentOrElse(sos -> {
                    sos.setStatus(status);
                    sosRequestRepository.save(sos);
                }, () -> {
                    throw new ResourceNotFoundException("SOS request not found: " + id);
                });
    }

    @Override
    public boolean existsById(UUID id) {
        return sosRequestRepository.existsById(id);
    }
}
