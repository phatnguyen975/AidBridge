package com.drc.aidbridge.modules.aid.internal;

import com.drc.aidbridge.modules.aid.AidItemCategoryDTO;
import com.drc.aidbridge.modules.shared.exception.ResourceNotFoundException;
import com.drc.aidbridge.modules.aid.AidFacade;
import com.drc.aidbridge.modules.aid.AidRequestDTO;
import com.drc.aidbridge.modules.aid.internal.mapper.AidMapper;
import com.drc.aidbridge.modules.aid.internal.repository.AidItemCategoryJpaRepository;
import com.drc.aidbridge.modules.aid.internal.repository.AidRequestJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.StreamSupport;

@Service
@RequiredArgsConstructor
public class AidFacadeImpl implements AidFacade {

    private final AidRequestJpaRepository aidRequestRepository;
    private final AidItemCategoryJpaRepository aidItemCategoryRepository;
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

    @Override
    public List<AidRequestDTO> findAllByStatus(com.drc.aidbridge.modules.shared.enums.AidStatus status) {
        return aidRequestRepository.findByStatus(status).stream()
                .map(aidMapper::toDTO)
                .toList();
    }

    @Override
    public List<AidRequestDTO> findAllByStatusAndDateRange(com.drc.aidbridge.modules.shared.enums.AidStatus status, java.time.Instant start, java.time.Instant end) {
        return aidRequestRepository.findByStatusAndCreatedAtBetween(status, start, end).stream()
                .map(aidMapper::toDTO)
                .toList();
    }

    @Override
    public List<AidItemCategoryDTO> findAllCategoriesById(Collection<UUID> ids) {
        return StreamSupport.stream(aidItemCategoryRepository.findAllById(ids).spliterator(), false)
                .map(aidMapper::toCategoryDTO)
                .toList();
    }
}
