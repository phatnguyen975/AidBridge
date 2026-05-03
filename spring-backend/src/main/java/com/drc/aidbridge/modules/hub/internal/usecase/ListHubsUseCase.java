package com.drc.aidbridge.modules.hub.internal.usecase;

import com.drc.aidbridge.modules.hub.HubDTO;
import com.drc.aidbridge.modules.hub.internal.mapper.HubMapper;
import com.drc.aidbridge.modules.hub.internal.repository.HubRepository;
import com.drc.aidbridge.modules.shared.enums.HubStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ListHubsUseCase {

    private final HubRepository hubRepository;
    private final HubMapper hubMapper;

    public List<HubDTO> execute(HubStatus status) {
        return execute(status, null);
    }

    public List<HubDTO> execute(HubStatus status, String keyword) {
        String statusStr = status != null ? status.name() : null;
        return hubRepository.searchHubs(statusStr, normalizeKeyword(keyword))
                .stream()
                .map(hubMapper::toDTO)
                .toList();
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null) {
            return null;
        }
        String trimmed = keyword.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
