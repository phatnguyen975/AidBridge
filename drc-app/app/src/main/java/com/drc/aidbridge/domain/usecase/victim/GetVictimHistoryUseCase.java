package com.drc.aidbridge.domain.usecase.victim;

import androidx.lifecycle.LiveData;

import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.domain.model.victim.VictimHistoryPage;
import com.drc.aidbridge.domain.repository.victim.VictimHistoryRepository;

import javax.inject.Inject;

public class GetVictimHistoryUseCase {

    private final VictimHistoryRepository victimHistoryRepository;

    @Inject
    public GetVictimHistoryUseCase(VictimHistoryRepository victimHistoryRepository) {
        this.victimHistoryRepository = victimHistoryRepository;
    }

    public LiveData<NetworkResultWrapper<VictimHistoryPage>> execute(int page,
                                                                      int size,
                                                                      String timeRange,
                                                                      String status,
                                                                      boolean forceOffline) {
        return victimHistoryRepository.getVictimHistory(page, size, timeRange, status, forceOffline);
    }
}
