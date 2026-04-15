package com.drc.aidbridge.data.remote.api;

import com.drc.aidbridge.data.remote.dto.request.RoutingRequestDto;
import com.drc.aidbridge.data.remote.dto.response.RoutingResponseDto;
import com.drc.aidbridge.data.remote.dto.response.BaseResponse;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

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

    /**
     * GET /api/routing/health
     * Health check for routing service.
     */
    @GET("routing/health")
    Call<BaseResponse<String>> healthCheck();
}
