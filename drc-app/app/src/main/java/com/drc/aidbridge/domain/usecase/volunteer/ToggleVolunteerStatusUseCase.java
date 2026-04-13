package com.drc.aidbridge.domain.usecase.volunteer;

import androidx.lifecycle.LiveData;

import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.data.remote.dto.request.volunteer.ToggleStatusRequest;
import com.drc.aidbridge.domain.repository.volunteer.VolunteerRepository;

import javax.inject.Inject;

public class ToggleVolunteerStatusUseCase {

    private final VolunteerRepository volunteerRepository;

    @Inject
    public ToggleVolunteerStatusUseCase(VolunteerRepository volunteerRepository) {
        this.volunteerRepository = volunteerRepository;
    }

    public LiveData<NetworkResultWrapper<Boolean>> execute(ToggleStatusRequest request) {
        return volunteerRepository.toggleStatus(request);
    }
}
