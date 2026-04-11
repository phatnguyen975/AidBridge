package com.drc.aidbridge.domain.usecase.victim;

import androidx.lifecycle.LiveData;

import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.data.remote.dto.supply.SupplyCategoryDto;
import com.drc.aidbridge.domain.repository.SupplyRepository;

import java.util.List;

import javax.inject.Inject;

public class GetSupplyCategoriesUseCase {

    private final SupplyRepository supplyRepository;

    @Inject
    public GetSupplyCategoriesUseCase(SupplyRepository supplyRepository) {
        this.supplyRepository = supplyRepository;
    }

    public LiveData<NetworkResultWrapper<List<SupplyCategoryDto>>> execute() {
        return supplyRepository.getSupplyCategories();
    }
}
