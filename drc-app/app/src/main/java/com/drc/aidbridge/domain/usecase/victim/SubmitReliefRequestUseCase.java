package com.drc.aidbridge.domain.usecase.victim;

import androidx.lifecycle.LiveData;

import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.domain.model.victim.VictimReliefRequest;
import com.drc.aidbridge.domain.repository.victim.VictimSupplyRepository;
import com.drc.aidbridge.domain.usecase.validation.ValidationResult;
import com.drc.aidbridge.domain.usecase.validation.VictimSupplyInputValidator;

import javax.inject.Inject;

public class SubmitReliefRequestUseCase {

    private final VictimSupplyRepository victimSupplyRepository;
    private final VictimSupplyInputValidator victimSupplyInputValidator;

    @Inject
    public SubmitReliefRequestUseCase(VictimSupplyRepository victimSupplyRepository,
                                      VictimSupplyInputValidator victimSupplyInputValidator) {
        this.victimSupplyRepository = victimSupplyRepository;
        this.victimSupplyInputValidator = victimSupplyInputValidator;
    }

    public ValidationResult validate(VictimReliefRequest request) {
        return victimSupplyInputValidator.validateReliefRequest(request);
    }

    public LiveData<NetworkResultWrapper<String>> execute(VictimReliefRequest request) {
        return victimSupplyRepository.submitReliefRequest(request);
    }
}
