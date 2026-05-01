package com.drc.aidbridge.modules.hub;

import com.drc.aidbridge.modules.hub.internal.web.dto.CreateHubRequest;
import com.drc.aidbridge.modules.hub.internal.web.dto.StockInHubInventoryRequest;
import com.drc.aidbridge.modules.hub.internal.web.dto.UpdateHubRequest;
import com.drc.aidbridge.modules.shared.enums.HubStatus;

import java.util.List;
import java.util.UUID;

public interface HubFacade {
    HubDTO getById(UUID id);
    List<HubDTO> list(HubStatus status);
    List<HubDTO> list(HubStatus status, String keyword);
    HubDTO create(CreateHubRequest request);
    HubDTO update(UUID id, UpdateHubRequest request);
    HubDTO stockIn(UUID id, StockInHubInventoryRequest request);
    List<HubDTO> listNearLocation(HubStatus status, double lat, double lon, double radius);

    long countTotalHubs();
    List<HubInventoryDTO> getAllInventories();
    HubStaffDTO assignStaff(UUID hubId, UUID userId);
    List<HubStaffDTO> findActiveAssignmentsByUserIds(List<UUID> userIds);
}
