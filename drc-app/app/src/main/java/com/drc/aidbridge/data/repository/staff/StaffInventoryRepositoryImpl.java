package com.drc.aidbridge.data.repository.staff;

import android.util.Log;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.data.remote.api.staff.StaffInventoryApiService;
import com.drc.aidbridge.data.remote.dto.response.BaseResponse;
import com.drc.aidbridge.data.remote.dto.response.staff.StaffInventoryResponseDto;
import com.drc.aidbridge.data.repository.BaseRepository;
import com.drc.aidbridge.domain.model.staff.StaffInventory;
import com.drc.aidbridge.domain.model.staff.StaffInventoryFilter;
import com.drc.aidbridge.domain.model.staff.StaffInventoryHub;
import com.drc.aidbridge.domain.model.staff.StaffInventoryItem;
import com.drc.aidbridge.domain.repository.staff.StaffInventoryRepository;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@Singleton
public class StaffInventoryRepositoryImpl extends BaseRepository implements StaffInventoryRepository {

    private static final String TAG = "StaffInventoryRepo";
    private static final String GENERIC_LOAD_ERROR =
            "Kh\u00f4ng th\u1ec3 t\u1ea3i danh s\u00e1ch t\u1ed3n kho. Vui l\u00f2ng th\u1eed l\u1ea1i.";
    private static final String STAFF_UNASSIGNED_ERROR =
            "Staff is not assigned to any active hub";

    private final StaffInventoryApiService apiService;

    @Inject
    public StaffInventoryRepositoryImpl(StaffInventoryApiService apiService) {
        this.apiService = apiService;
    }

    @Override
    public LiveData<NetworkResultWrapper<StaffInventory>> getMyHubInventory(@Nullable String parentCategoryId,
                                                                            @Nullable String parentCategoryName,
                                                                            @Nullable String keyword,
                                                                            int page,
                                                                            int size) {
        MutableLiveData<NetworkResultWrapper<StaffInventory>> result = new MutableLiveData<>();
        result.postValue(NetworkResultWrapper.loading());

        apiService.getMyHubInventory(
                trimToNull(parentCategoryId),
                trimToNull(parentCategoryName),
                trimToNull(keyword),
                Math.max(page, 0),
                size > 0 ? size : 50
        ).enqueue(new Callback<BaseResponse<StaffInventoryResponseDto>>() {
            @Override
            public void onResponse(Call<BaseResponse<StaffInventoryResponseDto>> call,
                                   Response<BaseResponse<StaffInventoryResponseDto>> response) {
                if (!response.isSuccessful()) {
                    String rawMessage = extractHttpError(response);
                    Log.w(TAG, "Inventory request failed: " + rawMessage);
                    result.postValue(NetworkResultWrapper.error(
                            toFriendlyError(rawMessage, response.code()),
                            response.code()
                    ));
                    return;
                }

                BaseResponse<StaffInventoryResponseDto> body = response.body();
                if (body == null) {
                    result.postValue(NetworkResultWrapper.error(GENERIC_LOAD_ERROR));
                    return;
                }

                if (!body.isSuccess()) {
                    String rawMessage = body.getMessage();
                    Log.w(TAG, "Inventory API returned error: " + rawMessage);
                    result.postValue(NetworkResultWrapper.error(toFriendlyError(rawMessage, 0)));
                    return;
                }

                result.postValue(NetworkResultWrapper.success(mapToDomain(body.getData())));
            }

            @Override
            public void onFailure(Call<BaseResponse<StaffInventoryResponseDto>> call, Throwable throwable) {
                Log.e(TAG, "Inventory request failed", throwable);
                result.postValue(NetworkResultWrapper.error(
                        "Kh\u00f4ng th\u1ec3 k\u1ebft n\u1ed1i m\u00e1y ch\u1ee7. Vui l\u00f2ng th\u1eed l\u1ea1i."));
            }
        });

        return result;
    }

    private StaffInventory mapToDomain(@Nullable StaffInventoryResponseDto dto) {
        if (dto == null) {
            return new StaffInventory(null, new ArrayList<>(), new ArrayList<>(), 0L);
        }

        return new StaffInventory(
                mapHub(dto.getHub()),
                mapFilters(dto.getFilters()),
                mapItems(dto.getItems()),
                dto.getTotalItems() != null ? dto.getTotalItems() : 0L
        );
    }

    private StaffInventoryHub mapHub(@Nullable StaffInventoryResponseDto.HubDto dto) {
        if (dto == null) {
            return null;
        }
        return new StaffInventoryHub(dto.getId(), dto.getName(), dto.getAddress());
    }

    private List<StaffInventoryFilter> mapFilters(@Nullable List<StaffInventoryResponseDto.FilterDto> dtos) {
        List<StaffInventoryFilter> filters = new ArrayList<>();
        if (dtos == null) {
            return filters;
        }

        for (StaffInventoryResponseDto.FilterDto dto : dtos) {
            if (dto == null) {
                continue;
            }
            filters.add(new StaffInventoryFilter(dto.getId(), dto.getName(), dto.getType()));
        }
        return filters;
    }

    private List<StaffInventoryItem> mapItems(@Nullable List<StaffInventoryResponseDto.ItemDto> dtos) {
        List<StaffInventoryItem> items = new ArrayList<>();
        if (dtos == null) {
            return items;
        }

        for (StaffInventoryResponseDto.ItemDto dto : dtos) {
            if (dto == null) {
                continue;
            }
            items.add(new StaffInventoryItem(
                    dto.getInventoryId(),
                    dto.getItemCategoryId(),
                    dto.getName(),
                    dto.getUnit(),
                    dto.getIconUrl(),
                    dto.getParentCategoryId(),
                    dto.getParentCategoryName(),
                    safeInt(dto.getCurrentQuantity()),
                    safeInt(dto.getLowStockThreshold()),
                    dto.isLowStock() != null && dto.isLowStock(),
                    dto.getLastRestockedAt()
            ));
        }
        return items;
    }

    private String toFriendlyError(@Nullable String rawMessage, int code) {
        String safe = rawMessage != null ? rawMessage.trim() : "";
        if (safe.contains(STAFF_UNASSIGNED_ERROR)) {
            return STAFF_UNASSIGNED_ERROR;
        }

        if (code == 403) {
            return "B\u1ea1n kh\u00f4ng c\u00f3 quy\u1ec1n truy c\u1eadp t\u1ed3n kho n\u00e0y.";
        }

        if (looksLikeRawServerError(safe)) {
            return GENERIC_LOAD_ERROR;
        }

        return safe.isEmpty() ? GENERIC_LOAD_ERROR : safe;
    }

    private boolean looksLikeRawServerError(String message) {
        String lower = message.toLowerCase();
        return lower.contains("jdbc exception")
                || lower.contains("sql")
                || lower.contains("select ")
                || lower.contains("lower(")
                || lower.contains("org.springframework")
                || lower.contains("exception");
    }

    private int safeInt(@Nullable Integer value) {
        return value == null ? 0 : Math.max(value, 0);
    }

    private String trimToNull(@Nullable String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
