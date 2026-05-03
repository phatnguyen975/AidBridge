package com.drc.aidbridge.domain.repository.volunteer;

import androidx.lifecycle.LiveData;

import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.data.remote.dto.request.volunteer.ToggleStatusRequest;
import com.drc.aidbridge.data.remote.dto.response.volunteer.VolunteerHistoryDataDto;
import com.drc.aidbridge.data.remote.dto.response.volunteer.VolunteerProfileDataDto;
import com.drc.aidbridge.data.remote.dto.response.volunteer.LatestDispatchDataDto;

public interface VolunteerRepository {

    LiveData<NetworkResultWrapper<VolunteerProfileDataDto>> getVolunteerDashboardInfo();

    LiveData<NetworkResultWrapper<VolunteerHistoryDataDto>> getMissionHistory(int page, int limit);

    LiveData<NetworkResultWrapper<com.drc.aidbridge.data.remote.dto.response.volunteer.MissionHistoryFullDataDto>> getMissionHistoryFull(int page, int limit);

    LiveData<NetworkResultWrapper<com.drc.aidbridge.data.remote.dto.response.volunteer.MissionHistoryFullItemDto>> getCurrentMission();

    LiveData<NetworkResultWrapper<com.drc.aidbridge.data.remote.dto.response.MissionDto>> completeMission(String missionId, String notes);

    LiveData<NetworkResultWrapper<com.drc.aidbridge.data.remote.dto.response.MissionDto>> cancelMission(String missionId, String reason);

    LiveData<NetworkResultWrapper<Boolean>> toggleStatus(ToggleStatusRequest request);

    LiveData<NetworkResultWrapper<LatestDispatchDataDto>> getLatestDispatch();

    LiveData<NetworkResultWrapper<LatestDispatchDataDto>> cancelDispatchAttempt(String dispatchAttemptId);

    LiveData<NetworkResultWrapper<LatestDispatchDataDto>> acceptDispatchAttempt(String dispatchAttemptId);
}
