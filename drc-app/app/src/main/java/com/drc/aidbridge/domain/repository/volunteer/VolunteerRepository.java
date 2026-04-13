package com.drc.aidbridge.domain.repository.volunteer;

import androidx.lifecycle.LiveData;

import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.data.remote.dto.request.volunteer.ToggleStatusRequest;
import com.drc.aidbridge.data.remote.dto.response.volunteer.VolunteerHistoryItemDto;
import com.drc.aidbridge.data.remote.dto.response.volunteer.VolunteerProfileDataDto;

import java.util.List;

public interface VolunteerRepository {

    LiveData<NetworkResultWrapper<VolunteerProfileDataDto>> getVolunteerDashboardInfo();

    LiveData<NetworkResultWrapper<List<VolunteerHistoryItemDto>>> getMissionHistory();

    LiveData<NetworkResultWrapper<Boolean>> toggleStatus(ToggleStatusRequest request);
}
