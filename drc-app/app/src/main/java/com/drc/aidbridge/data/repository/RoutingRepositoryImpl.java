package com.drc.aidbridge.data.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.drc.aidbridge.data.local.routing.OfflineRoutingLocalDataSource;
import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.data.remote.api.RoutingApiService;
import com.drc.aidbridge.data.remote.dto.request.RoutingRequestDto;
import com.drc.aidbridge.data.remote.dto.response.BaseResponse;
import com.drc.aidbridge.data.remote.dto.response.RoutingResponseDto;
import com.drc.aidbridge.domain.repository.RoutingRepository;
import com.drc.aidbridge.utils.NetworkUtils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@Singleton
public class RoutingRepositoryImpl extends BaseRepository implements RoutingRepository {

    private static final String OFFLINE_ROUTE_MISSING_MESSAGE =
            "Không có mạng và không có dữ liệu graph-data ngoại tuyến.";
    private static final String OFFLINE_ROUTE_FAILED_MESSAGE =
            "Có dữ liệu ngoại tuyến nhưng không thể tính lộ trình.";

    private final RoutingApiService routingApiService;
    private final OfflineRoutingLocalDataSource offlineRoutingLocalDataSource;
    private final Context appContext;
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();

    @Inject
    public RoutingRepositoryImpl(RoutingApiService routingApiService,
                                 OfflineRoutingLocalDataSource offlineRoutingLocalDataSource,
                                 @ApplicationContext Context appContext) {
        this.routingApiService = routingApiService;
        this.offlineRoutingLocalDataSource = offlineRoutingLocalDataSource;
        this.appContext = appContext;
    }

    @Override
    public LiveData<NetworkResultWrapper<RoutingResponseDto>> calculateRoute(RoutingRequestDto request) {
        MutableLiveData<NetworkResultWrapper<RoutingResponseDto>> result = new MutableLiveData<>();
        result.postValue(NetworkResultWrapper.loading());

        if (!NetworkUtils.isConnected(appContext)) {
            tryOfflineFallback(request, result, OFFLINE_ROUTE_FAILED_MESSAGE, 0);
            return result;
        }

        routingApiService.calculateRoute(request).enqueue(new Callback<BaseResponse<RoutingResponseDto>>() {
            @Override
            public void onResponse(Call<BaseResponse<RoutingResponseDto>> call,
                                   Response<BaseResponse<RoutingResponseDto>> response) {
                if (!response.isSuccessful()) {
                    tryOfflineFallback(request, result, extractHttpError(response), response.code());
                    return;
                }

                BaseResponse<RoutingResponseDto> baseResponse = response.body();
                if (baseResponse == null) {
                    tryOfflineFallback(
                            request,
                            result,
                            "Phản hồi tính toán lộ trình không hợp lệ.",
                            0
                    );
                    return;
                }

                if (!baseResponse.isSuccess()) {
                    String apiMessage = baseResponse.getMessage();
                    tryOfflineFallback(request, result,
                            apiMessage != null && !apiMessage.trim().isEmpty()
                                    ? apiMessage
                                    : "Không thể tính toán lộ trình.",
                            0
                    );
                    return;
                }

                RoutingResponseDto data = baseResponse.getData();
                if (data == null) {
                    String apiMessage = baseResponse.getMessage();
                    tryOfflineFallback(request, result,
                            apiMessage != null && !apiMessage.trim().isEmpty()
                                    ? apiMessage
                                    : "Không có dữ liệu lộ trình trả về.",
                            0
                    );
                    return;
                }

                data.setRouteSource(RoutingResponseDto.ROUTE_SOURCE_ONLINE);
                result.postValue(NetworkResultWrapper.success(data));
            }

            @Override
            public void onFailure(Call<BaseResponse<RoutingResponseDto>> call, Throwable t) {
                tryOfflineFallback(
                        request,
                        result,
                        "Không thể kết nối máy chủ: " + safeMessage(t),
                        0
                );
            }
        });

        return result;
    }

    private void tryOfflineFallback(RoutingRequestDto request,
                                    MutableLiveData<NetworkResultWrapper<RoutingResponseDto>> result,
                                    String errorMessage,
                                    int errorCode) {
        ioExecutor.execute(() -> {
            try {
                boolean isOfflineNow = !NetworkUtils.isConnected(appContext);
                boolean hasOfflineData = offlineRoutingLocalDataSource.hasGraphData();
                if (isOfflineNow && !hasOfflineData) {
                    result.postValue(NetworkResultWrapper.error(OFFLINE_ROUTE_MISSING_MESSAGE, errorCode));
                    return;
                }

                RoutingResponseDto offlineRoute = offlineRoutingLocalDataSource.calculateRoute(request);
                if (offlineRoute != null) {
                    offlineRoute.setRouteSource(RoutingResponseDto.ROUTE_SOURCE_OFFLINE);
                    result.postValue(NetworkResultWrapper.success(offlineRoute));
                    return;
                }

                if (isOfflineNow && hasOfflineData) {
                    result.postValue(NetworkResultWrapper.error(OFFLINE_ROUTE_FAILED_MESSAGE, errorCode));
                    return;
                }

                result.postValue(NetworkResultWrapper.error(errorMessage, errorCode));
            } catch (Throwable t) {
                result.postValue(NetworkResultWrapper.error(
                        OFFLINE_ROUTE_FAILED_MESSAGE + " (" + safeMessage(t) + ")",
                        errorCode
                ));
            }
        });
    }
}
