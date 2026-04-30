package com.drc.aidbridge.domain.usecase.volunteer;

import androidx.lifecycle.LiveData;
import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.data.remote.dto.response.volunteer.LatestDispatchDataDto;
import com.drc.aidbridge.domain.repository.volunteer.VolunteerRepository;
import javax.inject.Inject;

public class AcceptDispatchAttemptUseCase {
    private final VolunteerRepository volunteerRepository;

    @Inject
    public AcceptDispatchAttemptUseCase(VolunteerRepository volunteerRepository) {
        this.volunteerRepository = volunteerRepository;
    }

    public LiveData<NetworkResultWrapper<LatestDispatchDataDto>> execute(String dispatchAttemptId) {
        return volunteerRepository.acceptDispatchAttempt(dispatchAttemptId);
    }
}
