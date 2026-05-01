package com.drc.aidbridge.modules.hub.internal;

import com.drc.aidbridge.modules.hub.HubDTO;
import com.drc.aidbridge.modules.hub.HubFacade;
import com.drc.aidbridge.modules.hub.HubInventoryDTO;
import com.drc.aidbridge.modules.hub.HubStaffDTO;
import com.drc.aidbridge.modules.hub.internal.entity.HubStaff;
import com.drc.aidbridge.modules.hub.internal.mapper.HubMapper;
import com.drc.aidbridge.modules.hub.internal.mapper.HubStaffMapper;
import com.drc.aidbridge.modules.hub.internal.repository.HubInventoryRepository;
import com.drc.aidbridge.modules.hub.internal.repository.HubRepository;
import com.drc.aidbridge.modules.hub.internal.repository.HubStaffRepository;
import com.drc.aidbridge.modules.hub.internal.usecase.CreateHubUseCase;
import com.drc.aidbridge.modules.hub.internal.usecase.ListHubNearLocationUseCase;
import com.drc.aidbridge.modules.hub.internal.usecase.ListHubsUseCase;
import com.drc.aidbridge.modules.hub.internal.usecase.StockInHubInventoryUseCase;
import com.drc.aidbridge.modules.hub.internal.usecase.UpdateHubUseCase;
import com.drc.aidbridge.modules.hub.internal.web.dto.CreateHubRequest;
import com.drc.aidbridge.modules.hub.internal.web.dto.StockInHubInventoryRequest;
import com.drc.aidbridge.modules.hub.internal.web.dto.UpdateHubRequest;
import com.drc.aidbridge.modules.shared.enums.HubStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class HubFacadeImpl implements HubFacade {

    private final HubRepository hubRepository;
    private final HubInventoryRepository hubInventoryRepository;
    private final HubStaffRepository hubStaffRepository;
    private final HubMapper hubMapper;
    private final HubStaffMapper hubStaffMapper;
    private final ListHubsUseCase listHubsUseCase;
    private final ListHubNearLocationUseCase listHubNearLocationUseCase;
    private final CreateHubUseCase createHubUseCase;
    private final UpdateHubUseCase updateHubUseCase;
    private final StockInHubInventoryUseCase stockInHubInventoryUseCase;

    @Override
    public HubDTO getById(UUID id) {
        return hubRepository.findById(id).map(hubMapper::toDTO).orElse(null);
    }

    @Override
    public List<HubDTO> list(HubStatus status) {
        return listHubsUseCase.execute(status);
    }

    @Override
    public List<HubDTO> list(HubStatus status, String keyword) {
        return listHubsUseCase.execute(status, keyword);
    }

    @Override
    public HubDTO create(CreateHubRequest request) {
        return createHubUseCase.execute(request);
    }

    @Override
    public HubDTO update(UUID id, UpdateHubRequest request) {
        return updateHubUseCase.execute(id, request);
    }

    @Override
    public HubDTO stockIn(UUID id, StockInHubInventoryRequest request) {
        return stockInHubInventoryUseCase.execute(id, request);
    }

    @Override 
    public List<HubDTO> listNearLocation(HubStatus status, double lat, double lon, double radius) {
        return listHubNearLocationUseCase.execute(status, lat, lon, radius);
    }

    @Override
    public long countTotalHubs() {
        return hubRepository.count();
    }

    @Override
    public List<HubInventoryDTO> getAllInventories() {
        return hubInventoryRepository.findAll().stream()
                .map(hubMapper::toInventoryDTO)
                .toList();
    }

    @Override
    @Transactional
    public HubStaffDTO assignStaff(UUID hubId, UUID userId) {
        HubStaff assignment = HubStaff.builder()
                .hubId(hubId)
                .userId(userId)
                .isAvailable(true)
                .assignedAt(Instant.now())
                .build();
        return hubStaffMapper.toDTO(hubStaffRepository.save(assignment));
    }

    @Override
    public List<HubStaffDTO> findActiveAssignmentsByUserIds(List<UUID> userIds) {
        return hubStaffRepository.findByUserIdInAndUnassignedAtIsNull(userIds).stream()
                .map(hubStaffMapper::toDTO)
                .toList();
    }
}
