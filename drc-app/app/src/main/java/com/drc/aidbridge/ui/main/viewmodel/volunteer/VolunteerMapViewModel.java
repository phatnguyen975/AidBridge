package com.drc.aidbridge.ui.main.viewmodel.volunteer;

import com.drc.aidbridge.domain.repository.HubRepository;
import com.drc.aidbridge.domain.repository.RoutingRepository;
import com.drc.aidbridge.domain.usecase.routing.CalculateRouteUseCase;
import com.drc.aidbridge.ui.map.base.BaseMapViewModel;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class VolunteerMapViewModel extends BaseMapViewModel {

    @Inject
    public VolunteerMapViewModel(CalculateRouteUseCase calculateRouteUseCase,
                                 HubRepository hubRepository,
                                 RoutingRepository routingRepository) {
        super(calculateRouteUseCase, hubRepository, routingRepository);
    }
}