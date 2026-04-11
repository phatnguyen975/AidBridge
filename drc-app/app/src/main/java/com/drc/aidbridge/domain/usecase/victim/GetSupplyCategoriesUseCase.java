package com.drc.aidbridge.domain.usecase.victim;

import androidx.lifecycle.LiveData;

import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.domain.model.victim.VictimSupplyCategory;
import com.drc.aidbridge.domain.repository.victim.VictimSupplyRepository;

import java.util.List;

import javax.inject.Inject;

public class GetSupplyCategoriesUseCase {

    private final VictimSupplyRepository victimSupplyRepository;

    @Inject
    public GetSupplyCategoriesUseCase(VictimSupplyRepository victimSupplyRepository) {
        this.victimSupplyRepository = victimSupplyRepository;
    }

    public LiveData<NetworkResultWrapper<List<VictimSupplyCategory>>> execute() {
        return victimSupplyRepository.getSupplyCategories();
    }
}
