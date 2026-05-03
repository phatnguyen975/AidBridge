package com.drc.aidbridge.data.repository.admin;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.data.remote.api.admin.HubApiService;
import com.drc.aidbridge.data.remote.dto.request.admin.CreateHubRequest;
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
        return listHubs(null);
    }

    @Override
    public LiveData<NetworkResultWrapper<List<Hub>>> listHubs(String keyword) {
        MutableLiveData<NetworkResultWrapper<List<Hub>>> result = new MutableLiveData<>();
        result.postValue(NetworkResultWrapper.loading());

        hubApiService.getHubs(trimToNull(keyword)).enqueue(new Callback<BaseResponse<List<HubResponseDto>>>() {
            @Override
            public void onResponse(Call<BaseResponse<List<HubResponseDto>>> call,
                    Response<BaseResponse<List<HubResponseDto>>> response) {
                if (!response.isSuccessful()) {
                    result.postValue(NetworkResultWrapper.error(
                            extractHubHttpError(response, "Khong the tai danh sach tram."),
                            response.code()));
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
    public LiveData<NetworkResultWrapper<Hub>> createHub(String name,
            String address,
            String phoneNumber,
            String imageUrl,
            String operatingHours,
            Double latitude,
            Double longitude) {
        MutableLiveData<NetworkResultWrapper<Hub>> result = new MutableLiveData<>();
        result.postValue(NetworkResultWrapper.loading());

        CreateHubRequest request = new CreateHubRequest(
                safeText(name),
                safeText(address),
                safeText(phoneNumber),
                trimToNull(imageUrl),
                HubStatus.ACTIVE.name(),
                safeText(operatingHours),
                latitude,
                longitude);

        hubApiService.createHub(request).enqueue(new Callback<BaseResponse<HubResponseDto>>() {
            @Override
            public void onResponse(Call<BaseResponse<HubResponseDto>> call,
                    Response<BaseResponse<HubResponseDto>> response) {
                if (!response.isSuccessful()) {
                    result.postValue(NetworkResultWrapper.error(
                            extractHubHttpError(response, "Khong the them tram."),
                            response.code()));
                    return;
                }

                BaseResponse<HubResponseDto> baseResponse = response.body();
                if (baseResponse == null) {
                    result.postValue(NetworkResultWrapper.error("Phan hoi tao tram khong hop le."));
                    return;
                }

                if (!baseResponse.isSuccess()) {
                    String apiMessage = baseResponse.getMessage();
                    result.postValue(NetworkResultWrapper.error(
                            apiMessage != null && !apiMessage.trim().isEmpty()
                                    ? apiMessage
                                    : "Khong the them tram."));
                    return;
                }

                Hub mappedHub = mapToDomain(baseResponse.getData());
                if (mappedHub == null) {
                    result.postValue(NetworkResultWrapper.error("Du lieu tram vua tao khong hop le."));
                    return;
                }

                result.postValue(NetworkResultWrapper.success(mappedHub));
            }

            @Override
            public void onFailure(Call<BaseResponse<HubResponseDto>> call, Throwable throwable) {
                result.postValue(NetworkResultWrapper.error(
                        "Khong the ket noi may chu: " + safeMessage(throwable)));
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
                    result.postValue(NetworkResultWrapper.error(
                            extractHubHttpError(response, "Khong the cap nhat trang thai tram."),
                            response.code()));
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
                    result.postValue(NetworkResultWrapper.error(
                            extractHubHttpError(response, "Khong the tai chi tiet tram."),
                            response.code()));
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
        } else if (dto.getLatitude() != null || dto.getLongitude() != null) {
            location = new Hub.Location(dto.getLatitude(), dto.getLongitude());
        }

        List<Hub.InventoryGroup> inventoryGroups = new ArrayList<>();
        List<HubResponseDto.InventoryGroupDto> inventoryGroupDtos = dto.getInventoryGroups();
        if (inventoryGroupDtos != null) {
            for (HubResponseDto.InventoryGroupDto groupDto : inventoryGroupDtos) {
                if (groupDto == null) {
                    continue;
                }

                List<Hub.InventoryItem> inventoryItems = new ArrayList<>();
                List<HubResponseDto.InventoryItemDto> itemDtos = groupDto.getItems();
                if (itemDtos != null) {
                    for (HubResponseDto.InventoryItemDto itemDto : itemDtos) {
                        if (itemDto == null) {
                            continue;
                        }

                        inventoryItems.add(new Hub.InventoryItem(
                                parseUuid(itemDto.getItemCategoryId()),
                                safeText(itemDto.getItemCategoryName()),
                                safeText(itemDto.getUnit()),
                                safeInteger(itemDto.getCurrentQuantity()),
                                safeInteger(itemDto.getLowStockThreshold()),
                                safeText(itemDto.getLastRestockedAt())));
                    }
                }

                inventoryGroups.add(new Hub.InventoryGroup(
                        safeText(groupDto.getParentCategoryName()),
                        inventoryItems));
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
                inventoryGroups,
                safeLong(dto.getTotalImportedQuantity()),
                safeLong(dto.getTotalExportedQuantity()));
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

    private String trimToNull(String text) {
        String safe = safeText(text);
        return safe.isEmpty() ? null : safe;
    }

    private int safeInteger(Integer value) {
        return value == null ? 0 : Math.max(value, 0);
    }

    private long safeLong(Long value) {
        return value == null ? 0L : Math.max(value, 0L);
    }

    private String extractHubHttpError(Response<?> response, String fallback) {
        if (response != null && response.code() == 403) {
            return "Ban khong co quyen thuc hien thao tac nay";
        }

        String message = response != null ? extractHttpError(response) : "";
        if (message == null || message.trim().isEmpty()) {
            return fallback;
        }
        return message;
    }
}
