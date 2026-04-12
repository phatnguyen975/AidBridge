package com.drc.aidbridge.domain.repository.victim;

import androidx.lifecycle.LiveData;

import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.domain.model.victim.VictimHistoryDetail;
import com.drc.aidbridge.domain.model.victim.VictimHistoryPage;

public interface VictimHistoryRepository {

    LiveData<NetworkResultWrapper<VictimHistoryPage>> getVictimHistory(int page,
                                                                       int size,
                                                                       String timeRange,
                                                                       boolean forceOffline);

    LiveData<NetworkResultWrapper<VictimHistoryDetail>> getVictimHistoryDetail(String requestId,
                                                                                String type);
}
