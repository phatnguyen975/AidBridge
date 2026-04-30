package com.drc.aidbridge.domain.usecase.volunteer;

import androidx.lifecycle.LiveData;

import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.data.remote.dto.response.volunteer.MissionHistoryFullDataDto;
import com.drc.aidbridge.domain.repository.volunteer.VolunteerRepository;

import javax.inject.Inject;

public class GetVolunteerMissionHistoryFullUseCase {

    private final VolunteerRepository volunteerRepository;

    @Inject
    public GetVolunteerMissionHistoryFullUseCase(VolunteerRepository volunteerRepository) {
        this.volunteerRepository = volunteerRepository;
    }

    public LiveData<NetworkResultWrapper<MissionHistoryFullDataDto>> execute(int page, int limit) {
        return volunteerRepository.getMissionHistoryFull(page, limit);
    }
}
