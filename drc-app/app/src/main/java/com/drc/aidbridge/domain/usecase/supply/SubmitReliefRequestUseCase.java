package com.drc.aidbridge.domain.usecase.supply;

import androidx.lifecycle.LiveData;

import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.data.remote.dto.supply.ReliefRequestDto;
import com.drc.aidbridge.domain.repository.SupplyRepository;

import javax.inject.Inject;

public class SubmitReliefRequestUseCase {

    private final SupplyRepository supplyRepository;

    @Inject
    public SubmitReliefRequestUseCase(SupplyRepository supplyRepository) {
        this.supplyRepository = supplyRepository;
    }

    public LiveData<NetworkResultWrapper<String>> execute(ReliefRequestDto requestDto) {
        return supplyRepository.submitReliefRequest(requestDto);
    }
}
