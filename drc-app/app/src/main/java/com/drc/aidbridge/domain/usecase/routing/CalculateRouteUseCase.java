package com.drc.aidbridge.domain.usecase.routing;

import androidx.lifecycle.LiveData;

import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.data.remote.dto.request.RoutingRequestDto;
import com.drc.aidbridge.data.remote.dto.response.RoutingResponseDto;
import com.drc.aidbridge.domain.repository.RoutingRepository;

import javax.inject.Inject;

public class CalculateRouteUseCase {

	private final RoutingRepository routingRepository;

	@Inject
	public CalculateRouteUseCase(RoutingRepository routingRepository) {
		this.routingRepository = routingRepository;
	}

	public LiveData<NetworkResultWrapper<RoutingResponseDto>> execute(RoutingRequestDto request) {
		return routingRepository.calculateRoute(request);
	}
}
