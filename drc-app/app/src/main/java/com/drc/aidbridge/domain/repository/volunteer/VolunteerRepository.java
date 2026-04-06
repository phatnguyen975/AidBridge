package com.drc.aidbridge.domain.repository.volunteer;

import androidx.lifecycle.LiveData;

import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.data.remote.dto.request.volunteer.ToggleStatusRequest;
import com.drc.aidbridge.domain.model.volunteer.VolunteerDashboardInfo;

public interface VolunteerRepository {

    LiveData<NetworkResultWrapper<VolunteerDashboardInfo>> getVolunteerDashboardInfo();

    LiveData<NetworkResultWrapper<Boolean>> toggleStatus(ToggleStatusRequest request);
}
