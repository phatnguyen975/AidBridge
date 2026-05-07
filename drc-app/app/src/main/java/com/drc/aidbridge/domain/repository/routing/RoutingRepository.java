package com.drc.aidbridge.domain.repository;

import androidx.lifecycle.LiveData;

import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.data.remote.dto.request.DangerousZoneRequestDto;
import com.drc.aidbridge.data.remote.dto.request.RoutingRequestDto;
import com.drc.aidbridge.data.remote.dto.response.DangerousZoneResponseDto;
import com.drc.aidbridge.data.remote.dto.response.RoutingResponseDto;

import java.util.List;
import java.util.UUID;

public interface RoutingRepository {

    LiveData<NetworkResultWrapper<RoutingResponseDto>> calculateRoute(RoutingRequestDto request);

    LiveData<NetworkResultWrapper<List<DangerousZoneResponseDto>>> getDangerousZones();

    LiveData<NetworkResultWrapper<DangerousZoneResponseDto>> createDangerousZone(DangerousZoneRequestDto request);

    LiveData<NetworkResultWrapper<DangerousZoneResponseDto>> updateDangerousZone(UUID id, DangerousZoneRequestDto request);

    LiveData<NetworkResultWrapper<Void>> deleteDangerousZone(UUID id);
}
