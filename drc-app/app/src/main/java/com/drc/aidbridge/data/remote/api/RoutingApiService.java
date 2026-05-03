package com.drc.aidbridge.data.remote.api;

import com.drc.aidbridge.data.remote.dto.request.DangerousZoneRequestDto;
import com.drc.aidbridge.data.remote.dto.request.RoutingRequestDto;
import com.drc.aidbridge.data.remote.dto.response.DangerousZoneResponseDto;
import com.drc.aidbridge.data.remote.dto.response.RoutingResponseDto;
import com.drc.aidbridge.data.remote.dto.response.BaseResponse;
import retrofit2.Call;
import retrofit2.http.*;

import java.util.List;
import java.util.UUID;

/**
 * Routing API service contract.
 * Communicates with Spring backend /api/routing endpoints.
 */
public interface RoutingApiService {

    /**
     * POST /api/routing/calculate
     * Calculate driving route between two coordinates with strategy and optional dangerous zones.
     */
    @POST("routing/calculate")
    Call<BaseResponse<RoutingResponseDto>> calculateRoute(@Body RoutingRequestDto request);

    @GET("routing/dangerous-zones")
    Call<BaseResponse<List<DangerousZoneResponseDto>>> getDangerousZones();

    @POST("routing/dangerous-zones")
    Call<BaseResponse<DangerousZoneResponseDto>> createDangerousZone(@Body DangerousZoneRequestDto request);

    @PATCH("routing/dangerous-zones/{id}")
    Call<BaseResponse<DangerousZoneResponseDto>> updateDangerousZone(@Path("id") UUID id, @Body DangerousZoneRequestDto request);

    @DELETE("routing/dangerous-zones/{id}")
    Call<BaseResponse<Void>> deleteDangerousZone(@Path("id") UUID id);

    /**
     * GET /api/routing/health
     * Health check for routing service.
     */
    @GET("routing/health")
    Call<BaseResponse<String>> healthCheck();
}
