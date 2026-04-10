package com.drc.aidbridge.domain.repository.volunteer;

import androidx.lifecycle.LiveData;

import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.data.remote.dto.request.volunteer.ToggleStatusRequest;
import com.drc.aidbridge.data.remote.dto.response.volunteer.VolunteerProfileDataDto;

public interface VolunteerRepository {

    LiveData<NetworkResultWrapper<VolunteerProfileDataDto>> getVolunteerDashboardInfo();

    LiveData<NetworkResultWrapper<Boolean>> toggleStatus(ToggleStatusRequest request);
}
