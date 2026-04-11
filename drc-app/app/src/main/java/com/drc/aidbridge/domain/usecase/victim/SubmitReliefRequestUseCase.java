package com.drc.aidbridge.domain.usecase.victim;

import androidx.lifecycle.LiveData;

import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.data.remote.dto.supply.ReliefRequestDto;
import com.drc.aidbridge.domain.repository.SupplyRepository;
import com.drc.aidbridge.domain.usecase.validation.AuthValidationResult;
import com.drc.aidbridge.domain.usecase.validation.VictimSupplyInputValidator;

import javax.inject.Inject;

public class SubmitReliefRequestUseCase {

    private final SupplyRepository supplyRepository;
    private final VictimSupplyInputValidator victimSupplyInputValidator;

    @Inject
    public SubmitReliefRequestUseCase(SupplyRepository supplyRepository,
                                      VictimSupplyInputValidator victimSupplyInputValidator) {
        this.supplyRepository = supplyRepository;
        this.victimSupplyInputValidator = victimSupplyInputValidator;
    }

    public AuthValidationResult validate(ReliefRequestDto requestDto) {
        return victimSupplyInputValidator.validateReliefRequest(requestDto);
    }

    public LiveData<NetworkResultWrapper<String>> execute(ReliefRequestDto requestDto) {
        return supplyRepository.submitReliefRequest(requestDto);
    }
}
