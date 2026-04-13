package com.drc.aidbridge.domain.usecase.mission;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;

import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.domain.model.VolunteerMission;
import com.drc.aidbridge.domain.repository.MissionRepository;

import javax.inject.Inject;

public class AcceptVolunteerMissionUseCase {

    private final MissionRepository missionRepository;

    @Inject
    public AcceptVolunteerMissionUseCase(MissionRepository missionRepository) {
        this.missionRepository = missionRepository;
    }

    public LiveData<NetworkResultWrapper<VolunteerMission>> execute(String missionId,
                                                                    String dispatchAttemptId,
                                                                    @Nullable Double currentLat,
                                                                    @Nullable Double currentLng) {
        return missionRepository.acceptMission(
                normalize(missionId),
                normalize(dispatchAttemptId),
                currentLat,
                currentLng
        );
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
