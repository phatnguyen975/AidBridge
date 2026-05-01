package com.drc.aidbridge.modules.admin.internal.web;

import com.drc.aidbridge.modules.admin.internal.web.dto.AdminRoutingSosAidResponse;
import com.drc.aidbridge.modules.mission.MissionFacade;
import com.drc.aidbridge.modules.shared.enums.MissionStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Collections;

@RestController
@RequestMapping("/api/admin/routing")
@RequiredArgsConstructor
public class AdminRoutingController {

    private final MissionFacade missionFacade;

    @GetMapping("/sos-aid")
    public AdminRoutingSosAidResponse getSosAndAidByStatus(
            @RequestParam String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        MissionStatus missionStatus;
        try {
            missionStatus = MissionStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            // If status is not a valid MissionStatus, we can return empty or handle specifically
            return AdminRoutingSosAidResponse.builder()
                    .sosRequests(Collections.emptyList())
                    .aidRequests(Collections.emptyList())
                    .build();
        }

        Instant startInstant = startDate != null ? startDate.atStartOfDay().toInstant(ZoneOffset.UTC) : null;
        Instant endInstant = endDate != null ? endDate.atTime(23, 59, 59).toInstant(ZoneOffset.UTC) : null;

        return AdminRoutingSosAidResponse.builder()
                .sosRequests(missionFacade.findSosByStatusAndDateRange(missionStatus, startInstant, endInstant))
                .aidRequests(missionFacade.findAidByStatusAndDateRange(missionStatus, startInstant, endInstant))
                .build();
    }
}
