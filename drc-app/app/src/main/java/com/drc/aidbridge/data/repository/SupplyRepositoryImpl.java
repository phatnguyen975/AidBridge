package com.drc.aidbridge.data.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.data.remote.api.SupplyApiService;
import com.drc.aidbridge.data.remote.dto.response.BaseResponse;
import com.drc.aidbridge.data.remote.dto.supply.ReliefRequestDto;
import com.drc.aidbridge.data.remote.dto.supply.SupplyCategoryDto;
import com.drc.aidbridge.domain.repository.SupplyRepository;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@Singleton
public class SupplyRepositoryImpl extends BaseRepository implements SupplyRepository {

    private final SupplyApiService supplyApiService;

    @Inject
    public SupplyRepositoryImpl(SupplyApiService supplyApiService) {
        this.supplyApiService = supplyApiService;
    }

    @Override
    public LiveData<NetworkResultWrapper<List<SupplyCategoryDto>>> getSupplyCategories() {
        MutableLiveData<NetworkResultWrapper<List<SupplyCategoryDto>>> result = new MutableLiveData<>();
        result.postValue(NetworkResultWrapper.loading());

        supplyApiService.getSupplyCategories().enqueue(new Callback<BaseResponse<List<SupplyCategoryDto>>>() {
            @Override
            public void onResponse(Call<BaseResponse<List<SupplyCategoryDto>>> call,
                                   Response<BaseResponse<List<SupplyCategoryDto>>> response) {
                if (!response.isSuccessful()) {
                    result.postValue(NetworkResultWrapper.error(extractHttpError(response), response.code()));
                    return;
                }

                BaseResponse<List<SupplyCategoryDto>> body = response.body();
                if (body == null) {
                    result.postValue(NetworkResultWrapper.error("Phản hồi danh mục tiếp tế không hợp lệ."));
                    return;
                }

                if (!body.isSuccess()) {
                    String message = body.getMessage();
                    result.postValue(NetworkResultWrapper.error(
                        message != null && !message.trim().isEmpty()
                            ? message.trim()
                            : "Không thể tải danh mục tiếp tế."
                    ));
                    return;
                }

                List<SupplyCategoryDto> categories = body.getData();
                if (categories == null) {
                    categories = new ArrayList<>();
                }
                result.postValue(NetworkResultWrapper.success(categories));
            }

            @Override
            public void onFailure(Call<BaseResponse<List<SupplyCategoryDto>>> call, Throwable t) {
                result.postValue(NetworkResultWrapper.error("Tải danh mục tiếp tế thất bại: " + safeMessage(t)));
            }
        });

        return result;
    }

    @Override
    public LiveData<NetworkResultWrapper<String>> submitReliefRequest(ReliefRequestDto requestDto) {
        MutableLiveData<NetworkResultWrapper<String>> result = new MutableLiveData<>();
        result.postValue(NetworkResultWrapper.loading());

        supplyApiService.submitReliefRequest(requestDto).enqueue(new Callback<BaseResponse<String>>() {
            @Override
            public void onResponse(Call<BaseResponse<String>> call,
                                   Response<BaseResponse<String>> response) {
                if (!response.isSuccessful()) {
                    result.postValue(NetworkResultWrapper.error(extractHttpError(response), response.code()));
                    return;
                }

                BaseResponse<String> body = response.body();
                if (body == null) {
                    result.postValue(NetworkResultWrapper.error("Phản hồi gửi yêu cầu tiếp tế không hợp lệ."));
                    return;
                }

                if (!body.isSuccess()) {
                    String message = body.getMessage();
                    result.postValue(NetworkResultWrapper.error(
                        message != null && !message.trim().isEmpty()
                            ? message.trim()
                            : "Gửi yêu cầu tiếp tế thất bại."
                    ));
                    return;
                }

                String message = body.getData();
                if (message == null || message.trim().isEmpty()) {
                    message = body.getMessage();
                }
                if (message == null || message.trim().isEmpty()) {
                    message = "Gửi yêu cầu tiếp tế thành công.";
                }

                result.postValue(NetworkResultWrapper.success(message.trim()));
            }

            @Override
            public void onFailure(Call<BaseResponse<String>> call, Throwable t) {
                result.postValue(NetworkResultWrapper.error("Gửi yêu cầu tiếp tế thất bại: " + safeMessage(t)));
            }
        });

        return result;
    }
}
