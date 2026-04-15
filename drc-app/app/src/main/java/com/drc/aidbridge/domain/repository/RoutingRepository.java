package com.drc.aidbridge.domain.repository;

import androidx.lifecycle.LiveData;

import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.data.remote.dto.request.RoutingRequestDto;
import com.drc.aidbridge.data.remote.dto.response.RoutingResponseDto;

public interface RoutingRepository {

	LiveData<NetworkResultWrapper<RoutingResponseDto>> calculateRoute(RoutingRequestDto request);
}
