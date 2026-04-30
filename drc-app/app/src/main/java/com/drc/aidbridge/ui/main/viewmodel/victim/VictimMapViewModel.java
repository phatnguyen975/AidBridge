package com.drc.aidbridge.ui.main.viewmodel.victim;

import com.drc.aidbridge.domain.repository.HubRepository;
import com.drc.aidbridge.domain.usecase.routing.CalculateRouteUseCase;
import com.drc.aidbridge.ui.map.base.BaseMapViewModel;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class VictimMapViewModel extends BaseMapViewModel {

    @Inject
    public VictimMapViewModel(CalculateRouteUseCase calculateRouteUseCase,
                              HubRepository hubRepository) {
        super(calculateRouteUseCase, hubRepository);
    }
}
