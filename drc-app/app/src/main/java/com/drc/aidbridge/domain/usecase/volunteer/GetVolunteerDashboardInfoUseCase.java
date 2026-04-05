package com.drc.aidbridge.domain.usecase.volunteer;

import androidx.lifecycle.LiveData;

import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.domain.model.volunteer.VolunteerDashboardInfo;
import com.drc.aidbridge.domain.repository.volunteer.VolunteerRepository;

import javax.inject.Inject;

public class GetVolunteerDashboardInfoUseCase {

    private final VolunteerRepository volunteerRepository;

    @Inject
    public GetVolunteerDashboardInfoUseCase(VolunteerRepository volunteerRepository) {
        this.volunteerRepository = volunteerRepository;
    }

    public LiveData<NetworkResultWrapper<VolunteerDashboardInfo>> execute() {
        return volunteerRepository.getVolunteerDashboardInfo();
    }
}
