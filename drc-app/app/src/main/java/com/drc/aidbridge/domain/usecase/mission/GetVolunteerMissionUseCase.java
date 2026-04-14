package com.drc.aidbridge.domain.usecase.mission;

import androidx.lifecycle.LiveData;

import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.domain.model.VolunteerMission;
import com.drc.aidbridge.domain.repository.MissionRepository;

import javax.inject.Inject;

public class GetVolunteerMissionUseCase {

    private final MissionRepository missionRepository;

    @Inject
    public GetVolunteerMissionUseCase(MissionRepository missionRepository) {
        this.missionRepository = missionRepository;
    }

    public LiveData<NetworkResultWrapper<VolunteerMission>> execute(String missionId) {
        return missionRepository.getMission(normalize(missionId));
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
