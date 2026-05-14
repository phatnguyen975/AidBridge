package com.drc.aidbridge.data.mapper;

import com.drc.aidbridge.data.remote.dto.response.MissionDto;
import com.drc.aidbridge.domain.model.VolunteerMission;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class MissionMapper implements BaseMapper<MissionDto, VolunteerMission> {

    @Inject
    public MissionMapper() {
    }

    @Override
    public VolunteerMission mapToDomain(MissionDto dto) {
        if (dto == null) {
            return null;
        }

        String address = dto.getSosRequest() != null ? dto.getSosRequest().getAddress() : null;
        String note = dto.getSosRequest() != null ? dto.getSosRequest().getDescription() : null;

        return new VolunteerMission(
                dto.getId(),
                dto.getMissionType(),
                dto.getStatus(),
                dto.getCodeName(),
                dto.getVictimLat(),
                dto.getVictimLng(),
                dto.getPriorityScore(),
                address,
                note,
                dto.getComment(),
                null,
                null
        );
    }

    @Override
    public MissionDto mapToDto(VolunteerMission domainModel) {
        return null;
    }
}
