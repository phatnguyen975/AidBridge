package com.drc.aidbridge.data.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.data.remote.api.RoutingApiService;
import com.drc.aidbridge.data.remote.dto.request.RoutingRequestDto;
import com.drc.aidbridge.data.remote.dto.response.BaseResponse;
import com.drc.aidbridge.data.remote.dto.response.RoutingResponseDto;
import com.drc.aidbridge.domain.repository.RoutingRepository;

import javax.inject.Inject;
import javax.inject.Singleton;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@Singleton
public class RoutingRepositoryImpl extends BaseRepository implements RoutingRepository {

    private final RoutingApiService routingApiService;

    @Inject
    public RoutingRepositoryImpl(RoutingApiService routingApiService) {
        this.routingApiService = routingApiService;
    }

    @Override
    public LiveData<NetworkResultWrapper<RoutingResponseDto>> calculateRoute(RoutingRequestDto request) {
        MutableLiveData<NetworkResultWrapper<RoutingResponseDto>> result = new MutableLiveData<>();
        result.postValue(NetworkResultWrapper.loading());

        routingApiService.calculateRoute(request).enqueue(new Callback<BaseResponse<RoutingResponseDto>>() {
            @Override
            public void onResponse(Call<BaseResponse<RoutingResponseDto>> call,
                                   Response<BaseResponse<RoutingResponseDto>> response) {
                if (!response.isSuccessful()) {
                    result.postValue(NetworkResultWrapper.error(extractHttpError(response), response.code()));
                    return;
                }

                BaseResponse<RoutingResponseDto> baseResponse = response.body();
                if (baseResponse == null) {
                    result.postValue(NetworkResultWrapper.error("Phản hồi tính toán lộ trình không hợp lệ."));
                    return;
                }

                if (!baseResponse.isSuccess()) {
                    String apiMessage = baseResponse.getMessage();
                    result.postValue(NetworkResultWrapper.error(
                            apiMessage != null && !apiMessage.trim().isEmpty()
                                    ? apiMessage
                                    : "Không thể tính toán lộ trình."
                    ));
                    return;
                }

                RoutingResponseDto data = baseResponse.getData();
                if (data == null) {
                    String apiMessage = baseResponse.getMessage();
                    result.postValue(NetworkResultWrapper.error(
                            apiMessage != null && !apiMessage.trim().isEmpty()
                                    ? apiMessage
                                    : "Không có dữ liệu lộ trình trả về."
                    ));
                    return;
                }

                result.postValue(NetworkResultWrapper.success(data));
            }

            @Override
            public void onFailure(Call<BaseResponse<RoutingResponseDto>> call, Throwable t) {
                result.postValue(NetworkResultWrapper.error("Không thể kết nối máy chủ: " + safeMessage(t)));
            }
        });

        return result;
    }
}
