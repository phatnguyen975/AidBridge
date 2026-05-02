package com.drc.aidbridge.modules.admin.internal.web.dto;

import com.drc.aidbridge.modules.aid.AidRequestDTO;
import com.drc.aidbridge.modules.sos.SosDTO;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AdminRoutingSosAidResponse {
    private List<SosDTO> sosRequests;
    private List<AidRequestDTO> aidRequests;
}
