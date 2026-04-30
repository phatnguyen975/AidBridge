package com.drc.aidbridge.domain.usecase.victim;

import androidx.lifecycle.LiveData;

import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.domain.model.victim.VictimVoiceReliefRequest;
import com.drc.aidbridge.domain.repository.victim.VictimSupplyRepository;

import javax.inject.Inject;

public class SubmitVoiceReliefRequestUseCase {

    private final VictimSupplyRepository victimSupplyRepository;

    @Inject
    public SubmitVoiceReliefRequestUseCase(VictimSupplyRepository victimSupplyRepository) {
        this.victimSupplyRepository = victimSupplyRepository;
    }

    public LiveData<NetworkResultWrapper<String>> execute(VictimVoiceReliefRequest request) {
        return victimSupplyRepository.submitVoiceReliefRequest(request);
    }
}
