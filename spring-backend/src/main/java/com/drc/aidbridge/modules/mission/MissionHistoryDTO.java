package com.drc.aidbridge.modules.mission;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MissionHistoryDTO {
    private String missionType;
    private Instant completedAt;
    private String address;
}
