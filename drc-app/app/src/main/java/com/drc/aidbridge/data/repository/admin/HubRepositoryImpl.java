package com.drc.aidbridge.data.repository.admin;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.data.remote.api.admin.HubApiService;
import com.drc.aidbridge.data.remote.dto.request.admin.UpdateHubStatusRequest;
import com.drc.aidbridge.data.remote.dto.response.BaseResponse;
import com.drc.aidbridge.data.remote.dto.response.admin.HubResponseDto;
import com.drc.aidbridge.data.repository.BaseRepository;
import com.drc.aidbridge.domain.enums.HubStatus;
import com.drc.aidbridge.domain.model.admin.Hub;
import com.drc.aidbridge.domain.repository.admin.HubRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Singleton;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@Singleton
public class HubRepositoryImpl extends BaseRepository implements HubRepository {

    private final HubApiService hubApiService;

    @Inject
    public HubRepositoryImpl(HubApiService hubApiService) {
        this.hubApiService = hubApiService;
    }

    @Override
    public LiveData<NetworkResultWrapper<List<Hub>>> listHubs() {
        MutableLiveData<NetworkResultWrapper<List<Hub>>> result = new MutableLiveData<>();
        result.postValue(NetworkResultWrapper.loading());

        hubApiService.getHubs().enqueue(new Callback<BaseResponse<List<HubResponseDto>>>() {
            @Override
            public void onResponse(Call<BaseResponse<List<HubResponseDto>>> call,
                    Response<BaseResponse<List<HubResponseDto>>> response) {
                if (!response.isSuccessful()) {
                    result.postValue(NetworkResultWrapper.error(extractHttpError(response), response.code()));
                    return;
                }

                BaseResponse<List<HubResponseDto>> baseResponse = response.body();
                if (baseResponse == null) {
                    result.postValue(NetworkResultWrapper.error("Phản hồi danh sách trạm không hợp lệ."));
                    return;
                }

                if (!baseResponse.isSuccess()) {
                    String apiMessage = baseResponse.getMessage();
                    result.postValue(NetworkResultWrapper.error(
                            apiMessage != null && !apiMessage.trim().isEmpty()
                                    ? apiMessage
                                    : "Không thể tải danh sách trạm."));
                    return;
                }

                List<HubResponseDto> dtoList = baseResponse.getData();
                List<Hub> mapped = new ArrayList<>();
                if (dtoList != null) {
                    for (HubResponseDto dto : dtoList) {
                        Hub mappedHub = mapToDomain(dto);
                        if (mappedHub != null) {
                            mapped.add(mappedHub);
                        }
                    }
                }

                result.postValue(NetworkResultWrapper.success(mapped));
            }

            @Override
            public void onFailure(Call<BaseResponse<List<HubResponseDto>>> call, Throwable throwable) {
                result.postValue(NetworkResultWrapper.error(
                        "Không thể kết nối máy chủ: " + safeMessage(throwable)));
            }
        });

        return result;
    }

    @Override
    public LiveData<NetworkResultWrapper<Hub>> updateHubStatus(UUID hubId, HubStatus status) {
        MutableLiveData<NetworkResultWrapper<Hub>> result = new MutableLiveData<>();
        result.postValue(NetworkResultWrapper.loading());

        UpdateHubStatusRequest request = new UpdateHubStatusRequest(status.name());
        hubApiService.updateHubStatus(hubId, request).enqueue(new Callback<BaseResponse<HubResponseDto>>() {
            @Override
            public void onResponse(Call<BaseResponse<HubResponseDto>> call,
                    Response<BaseResponse<HubResponseDto>> response) {
                if (!response.isSuccessful()) {
                    result.postValue(NetworkResultWrapper.error(extractHttpError(response), response.code()));
                    return;
                }

                BaseResponse<HubResponseDto> baseResponse = response.body();
                if (baseResponse == null) {
                    result.postValue(NetworkResultWrapper.error("Phản hồi cập nhật trạng thái trạm không hợp lệ."));
                    return;
                }

                if (!baseResponse.isSuccess()) {
                    String apiMessage = baseResponse.getMessage();
                    result.postValue(NetworkResultWrapper.error(
                            apiMessage != null && !apiMessage.trim().isEmpty()
                                    ? apiMessage
                                    : "Không thể cập nhật trạng thái trạm."));
                    return;
                }

                HubResponseDto data = baseResponse.getData();
                Hub mappedHub = mapToDomain(data);
                if (mappedHub == null) {
                    result.postValue(NetworkResultWrapper.error("Dữ liệu trạm sau cập nhật không hợp lệ."));
                    return;
                }

                result.postValue(NetworkResultWrapper.success(mappedHub));
            }

            @Override
            public void onFailure(Call<BaseResponse<HubResponseDto>> call, Throwable throwable) {
                result.postValue(NetworkResultWrapper.error(
                        "Không thể kết nối máy chủ: " + safeMessage(throwable)));
            }
        });

        return result;
    }

    @Override
    public LiveData<NetworkResultWrapper<Hub>> getHubDetail(UUID hubId) {
        MutableLiveData<NetworkResultWrapper<Hub>> result = new MutableLiveData<>();
        result.postValue(NetworkResultWrapper.loading());

        hubApiService.getHubById(hubId).enqueue(new Callback<BaseResponse<HubResponseDto>>() {
            @Override
            public void onResponse(Call<BaseResponse<HubResponseDto>> call,
                    Response<BaseResponse<HubResponseDto>> response) {
                if (!response.isSuccessful()) {
                    result.postValue(NetworkResultWrapper.error(extractHttpError(response), response.code()));
                    return;
                }

                BaseResponse<HubResponseDto> baseResponse = response.body();
                if (baseResponse == null) {
                    result.postValue(NetworkResultWrapper.error("Phản hồi chi tiết trạm không hợp lệ."));
                    return;
                }

                if (!baseResponse.isSuccess()) {
                    String apiMessage = baseResponse.getMessage();
                    result.postValue(NetworkResultWrapper.error(
                            apiMessage != null && !apiMessage.trim().isEmpty()
                                    ? apiMessage
                                    : "Không thể tải chi tiết trạm."));
                    return;
                }

                HubResponseDto data = baseResponse.getData();
                Hub mappedHub = mapToDomain(data);
                if (mappedHub == null) {
                    result.postValue(NetworkResultWrapper.error("Dữ liệu chi tiết trạm không hợp lệ."));
                    return;
                }

                result.postValue(NetworkResultWrapper.success(mappedHub));
            }

            @Override
            public void onFailure(Call<BaseResponse<HubResponseDto>> call, Throwable throwable) {
                result.postValue(NetworkResultWrapper.error(
                        "Không thể kết nối máy chủ: " + safeMessage(throwable)));
            }
        });

        return result;
    }

    private Hub mapToDomain(HubResponseDto dto) {
        if (dto == null) {
            return null;
        }

        UUID hubId = parseUuid(dto.getId());
        if (hubId == null) {
            return null;
        }

        HubResponseDto.LocationDto locationDto = dto.getLocation();
        Hub.Location location = null;
        if (locationDto != null) {
            location = new Hub.Location(locationDto.getLat(), locationDto.getLng());
        }

        List<Hub.InventoryItem> inventoryItems = new ArrayList<>();
        List<HubResponseDto.InventoryItemDto> inventoryDtos = dto.getInventory();
        if (inventoryDtos != null) {
            for (HubResponseDto.InventoryItemDto inventoryDto : inventoryDtos) {
                if (inventoryDto == null) {
                    continue;
                }

                inventoryItems.add(new Hub.InventoryItem(
                        parseUuid(inventoryDto.getItemCategoryId()),
                        safeText(inventoryDto.getItemCategoryName()),
                        safeInteger(inventoryDto.getCurrentQuantity()),
                        safeInteger(inventoryDto.getLowStockThreshold()),
                        safeText(inventoryDto.getLastRestockedAt())));
            }
        }

        return new Hub(
                hubId,
                safeText(dto.getName()),
                safeText(dto.getAddress()),
                safeText(dto.getPhoneNumber()),
                safeText(dto.getImageUrl()),
                safeText(dto.getStatus()),
                safeText(dto.getOperatingHours()),
                safeText(dto.getCreatedAt()),
                safeText(dto.getUpdatedAt()),
                location,
                inventoryItems);
    }

    private UUID parseUuid(String rawId) {
        if (rawId == null || rawId.trim().isEmpty()) {
            return null;
        }

        try {
            return UUID.fromString(rawId.trim());
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private String safeText(String text) {
        return text == null ? "" : text.trim();
    }

    private int safeInteger(Integer value) {
        return value == null ? 0 : Math.max(value, 0);
    }
}
