package com.drc.aidbridge.domain.usecase.victim;

import androidx.lifecycle.LiveData;

import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.domain.model.victim.VictimHistoryDetail;
import com.drc.aidbridge.domain.repository.victim.VictimHistoryRepository;

import javax.inject.Inject;

public class GetVictimHistoryDetailUseCase {

    private final VictimHistoryRepository victimHistoryRepository;

    @Inject
    public GetVictimHistoryDetailUseCase(VictimHistoryRepository victimHistoryRepository) {
        this.victimHistoryRepository = victimHistoryRepository;
    }

    public LiveData<NetworkResultWrapper<VictimHistoryDetail>> execute(String requestId,
                                                                        String type) {
        return victimHistoryRepository.getVictimHistoryDetail(requestId, type);
    }
}
