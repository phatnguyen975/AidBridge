package com.drc.aidbridge.domain.usecase.volunteer;

import androidx.lifecycle.LiveData;

import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.data.remote.dto.response.volunteer.MissionHistoryFullItemDto;
import com.drc.aidbridge.domain.repository.volunteer.VolunteerRepository;

import javax.inject.Inject;

public class GetCurrentVolunteerMissionUseCase {

    private final VolunteerRepository volunteerRepository;

    @Inject
    public GetCurrentVolunteerMissionUseCase(VolunteerRepository volunteerRepository) {
        this.volunteerRepository = volunteerRepository;
    }

    public LiveData<NetworkResultWrapper<MissionHistoryFullItemDto>> execute() {
        return volunteerRepository.getCurrentMission();
    }
}
