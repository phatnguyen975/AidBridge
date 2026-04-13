package com.drc.aidbridge.domain.usecase.mission;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;

import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.domain.repository.MissionRepository;

import javax.inject.Inject;

public class RejectVolunteerMissionUseCase {

    private final MissionRepository missionRepository;

    @Inject
    public RejectVolunteerMissionUseCase(MissionRepository missionRepository) {
        this.missionRepository = missionRepository;
    }

    public LiveData<NetworkResultWrapper<Boolean>> execute(String missionId,
                                                           String dispatchAttemptId,
                                                           String reason,
                                                           @Nullable String reasonDetail) {
        return missionRepository.rejectMission(
                normalize(missionId),
                normalize(dispatchAttemptId),
                normalize(reason),
                normalizeNullable(reasonDetail)
        );
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    @Nullable
    private String normalizeNullable(@Nullable String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
