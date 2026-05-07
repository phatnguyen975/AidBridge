package com.drc.aidbridge.domain.repository;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;

import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.domain.model.VolunteerMission;

public interface MissionRepository {

    LiveData<NetworkResultWrapper<VolunteerMission>> getMission(String missionId);

    LiveData<NetworkResultWrapper<VolunteerMission>> acceptMission(String missionId,
                                                                  String dispatchAttemptId,
                                                                  @Nullable Double currentLat,
                                                                  @Nullable Double currentLng);

    LiveData<NetworkResultWrapper<Boolean>> rejectMission(String missionId,
                                                          String dispatchAttemptId,
                                                          String reason,
                                                          @Nullable String reasonDetail);
}
