package com.drc.aidbridge.modules.volunteer.internal.web.dto;

import com.drc.aidbridge.modules.mission.MissionDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data 
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VolunteerMissionResponse {
    private MissionDTO currentMission;
}
