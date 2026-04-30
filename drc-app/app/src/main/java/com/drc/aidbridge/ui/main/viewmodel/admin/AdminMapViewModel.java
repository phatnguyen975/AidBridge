package com.drc.aidbridge.ui.main.viewmodel.admin;

import com.drc.aidbridge.domain.repository.HubRepository;
import com.drc.aidbridge.domain.usecase.routing.CalculateRouteUseCase;
import com.drc.aidbridge.ui.map.base.BaseMapViewModel;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class AdminMapViewModel extends BaseMapViewModel {

    @Inject
    public AdminMapViewModel(CalculateRouteUseCase calculateRouteUseCase,
                             HubRepository hubRepository) {
        super(calculateRouteUseCase, hubRepository);
    }
}
