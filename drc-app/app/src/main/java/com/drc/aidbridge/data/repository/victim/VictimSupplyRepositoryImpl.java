package com.drc.aidbridge.data.repository.victim;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.drc.aidbridge.data.mapper.victim.VictimSupplyMapper;
import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.data.remote.api.victim.SupplyApiService;
import com.drc.aidbridge.data.remote.dto.request.victim.ReliefRequest;
import com.drc.aidbridge.data.remote.dto.response.BaseResponse;
import com.drc.aidbridge.data.remote.dto.response.victim.SupplyCategoryResponse;
import com.drc.aidbridge.data.repository.BaseRepository;
import com.drc.aidbridge.domain.model.victim.VictimReliefRequest;
import com.drc.aidbridge.domain.model.victim.VictimSupplyCategory;
import com.drc.aidbridge.domain.repository.victim.VictimSupplyRepository;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@Singleton
public class VictimSupplyRepositoryImpl extends BaseRepository implements VictimSupplyRepository {

    private final SupplyApiService supplyApiService;
    private final VictimSupplyMapper victimSupplyMapper;

    @Inject
    public VictimSupplyRepositoryImpl(SupplyApiService supplyApiService,
                                      VictimSupplyMapper victimSupplyMapper) {
        this.supplyApiService = supplyApiService;
        this.victimSupplyMapper = victimSupplyMapper;
    }

    @Override
    public LiveData<NetworkResultWrapper<List<VictimSupplyCategory>>> getSupplyCategories() {
        MutableLiveData<NetworkResultWrapper<List<VictimSupplyCategory>>> result = new MutableLiveData<>();
        result.postValue(NetworkResultWrapper.loading());

        supplyApiService.getSupplyCategories().enqueue(new Callback<BaseResponse<List<SupplyCategoryResponse>>>() {
            @Override
            public void onResponse(Call<BaseResponse<List<SupplyCategoryResponse>>> call,
                                   Response<BaseResponse<List<SupplyCategoryResponse>>> response) {
                if (!response.isSuccessful()) {
                    result.postValue(NetworkResultWrapper.error(extractHttpError(response), response.code()));
                    return;
                }

                BaseResponse<List<SupplyCategoryResponse>> body = response.body();
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

                List<SupplyCategoryResponse> categories = body.getData();
                List<VictimSupplyCategory> domainCategories = victimSupplyMapper.mapCategoriesToDomain(
                    categories != null ? categories : Collections.emptyList()
                );
                result.postValue(NetworkResultWrapper.success(domainCategories));
            }

            @Override
            public void onFailure(Call<BaseResponse<List<SupplyCategoryResponse>>> call, Throwable t) {
                result.postValue(NetworkResultWrapper.error("Tải danh mục tiếp tế thất bại: " + safeMessage(t)));
            }
        });

        return result;
    }

    @Override
    public LiveData<NetworkResultWrapper<String>> submitReliefRequest(VictimReliefRequest request) {
        MutableLiveData<NetworkResultWrapper<String>> result = new MutableLiveData<>();
        result.postValue(NetworkResultWrapper.loading());

        ReliefRequest apiRequest = victimSupplyMapper.mapReliefRequestToRequest(request);

        supplyApiService.submitReliefRequest(apiRequest).enqueue(new Callback<BaseResponse<String>>() {
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
