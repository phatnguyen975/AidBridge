package com.drc.aidbridge.data.repository.staff;

import android.util.Log;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.data.remote.api.staff.StaffInventoryApiService;
import com.drc.aidbridge.data.remote.dto.request.staff.ConfirmInboundInventoryItemRequestDto;
import com.drc.aidbridge.data.remote.dto.request.staff.ConfirmInboundInventoryRequestDto;
import com.drc.aidbridge.data.remote.dto.request.staff.ConfirmInventoryItemRequestDto;
import com.drc.aidbridge.data.remote.dto.request.staff.ConfirmInventoryRequestDto;
import com.drc.aidbridge.data.remote.dto.request.staff.CreateInboundSubCategoryRequestDto;
import com.drc.aidbridge.data.remote.dto.response.BaseResponse;
import com.drc.aidbridge.data.remote.dto.response.staff.CreateInboundSubCategoryResponseDto;
import com.drc.aidbridge.data.remote.dto.response.staff.InboundDonationPreviewResponseDto;
import com.drc.aidbridge.data.remote.dto.response.staff.InboundSubCategoryDto;
import com.drc.aidbridge.data.remote.dto.response.staff.InventoryQrPreviewItemDto;
import com.drc.aidbridge.data.remote.dto.response.staff.InventoryQrPreviewResponseDto;
import com.drc.aidbridge.data.remote.dto.response.staff.InventoryTransactionItemDto;
import com.drc.aidbridge.data.remote.dto.response.staff.InventoryTransactionResponseDto;
import com.drc.aidbridge.data.remote.dto.response.staff.OutboundAidRequestDetailDto;
import com.drc.aidbridge.data.remote.dto.response.staff.OutboundAidRequestItemDto;
import com.drc.aidbridge.data.remote.dto.response.staff.SearchInboundSubCategoriesResponseDto;
import com.drc.aidbridge.domain.model.staff.InventoryConfirmItem;
import com.drc.aidbridge.domain.model.staff.InventoryQrPreview;
import com.drc.aidbridge.domain.model.staff.InventoryQrPreviewItem;
import com.drc.aidbridge.domain.model.staff.InventoryTransactionItem;
import com.drc.aidbridge.domain.model.staff.InventoryTransactionResult;
import com.drc.aidbridge.data.remote.dto.response.staff.StaffInventoryResponseDto;
import com.drc.aidbridge.data.repository.BaseRepository;
import com.drc.aidbridge.domain.model.staff.InboundDonationPreview;
import com.drc.aidbridge.domain.model.staff.InboundDraftItem;
import com.drc.aidbridge.domain.model.staff.InboundParentCategory;
import com.drc.aidbridge.domain.model.staff.InboundSubCategory;
import com.drc.aidbridge.domain.model.staff.OutboundAidRequestDetail;
import com.drc.aidbridge.domain.model.staff.OutboundAidRequestItem;
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
    private static final String GENERIC_TRANSACTION_ERROR =
            "Kh\u00f4ng th\u1ec3 x\u1eed l\u00fd giao d\u1ecbch t\u1ed3n kho. Vui l\u00f2ng th\u1eed l\u1ea1i.";
    private static final String NETWORK_ERROR =
            "Kh\u00f4ng th\u1ec3 k\u1ebft n\u1ed1i m\u00e1y ch\u1ee7. Vui l\u00f2ng th\u1eed l\u1ea1i.";
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

    @Override
    public LiveData<NetworkResultWrapper<InboundDonationPreview>> previewInbound(String code) {
        MutableLiveData<NetworkResultWrapper<InboundDonationPreview>> result = new MutableLiveData<>();
        result.postValue(NetworkResultWrapper.loading());

        apiService.previewInbound(trimToNull(code)).enqueue(new Callback<BaseResponse<InboundDonationPreviewResponseDto>>() {
            @Override
            public void onResponse(Call<BaseResponse<InboundDonationPreviewResponseDto>> call,
                                   Response<BaseResponse<InboundDonationPreviewResponseDto>> response) {
                handleInboundPreviewResponse(response, result);
            }

            @Override
            public void onFailure(Call<BaseResponse<InboundDonationPreviewResponseDto>> call, Throwable throwable) {
                Log.e(TAG, "Inbound preview failed", throwable);
                result.postValue(NetworkResultWrapper.error(NETWORK_ERROR));
            }
        });

        return result;
    }

    @Override
    public LiveData<NetworkResultWrapper<List<InboundSubCategory>>> searchInboundSubCategories(String donationId,
                                                                                               String parentCategoryId,
                                                                                               @Nullable String keyword) {
        MutableLiveData<NetworkResultWrapper<List<InboundSubCategory>>> result = new MutableLiveData<>();
        result.postValue(NetworkResultWrapper.loading());

        apiService.searchInboundSubCategories(
                trimToNull(donationId),
                trimToNull(parentCategoryId),
                trimToNull(keyword)
        ).enqueue(new Callback<BaseResponse<SearchInboundSubCategoriesResponseDto>>() {
            @Override
            public void onResponse(Call<BaseResponse<SearchInboundSubCategoriesResponseDto>> call,
                                   Response<BaseResponse<SearchInboundSubCategoriesResponseDto>> response) {
                if (!response.isSuccessful()) {
                    String rawMessage = extractHttpError(response);
                    result.postValue(NetworkResultWrapper.error(
                            toFriendlyTransactionError(rawMessage, response.code()),
                            response.code()
                    ));
                    return;
                }
                BaseResponse<SearchInboundSubCategoriesResponseDto> body = response.body();
                if (body == null || !body.isSuccess()) {
                    result.postValue(NetworkResultWrapper.error(toFriendlyTransactionError(
                            body != null ? body.getMessage() : null,
                            0
                    )));
                    return;
                }
                SearchInboundSubCategoriesResponseDto data = body.getData();
                result.postValue(NetworkResultWrapper.success(mapInboundSubCategories(
                        data != null ? data.getItems() : null
                )));
            }

            @Override
            public void onFailure(Call<BaseResponse<SearchInboundSubCategoriesResponseDto>> call, Throwable throwable) {
                Log.e(TAG, "Inbound sub-category search failed", throwable);
                result.postValue(NetworkResultWrapper.error(NETWORK_ERROR));
            }
        });

        return result;
    }

    @Override
    public LiveData<NetworkResultWrapper<InboundSubCategory>> createInboundSubCategory(String donationId,
                                                                                      String parentCategoryId,
                                                                                      String name,
                                                                                      String unit,
                                                                                      @Nullable String iconUrl) {
        MutableLiveData<NetworkResultWrapper<InboundSubCategory>> result = new MutableLiveData<>();
        result.postValue(NetworkResultWrapper.loading());

        CreateInboundSubCategoryRequestDto request = new CreateInboundSubCategoryRequestDto(
                trimToNull(donationId),
                trimToNull(parentCategoryId),
                trimToNull(name),
                trimToNull(unit),
                trimToNull(iconUrl)
        );
        apiService.createInboundSubCategory(request)
                .enqueue(new Callback<BaseResponse<CreateInboundSubCategoryResponseDto>>() {
                    @Override
                    public void onResponse(Call<BaseResponse<CreateInboundSubCategoryResponseDto>> call,
                                           Response<BaseResponse<CreateInboundSubCategoryResponseDto>> response) {
                        if (!response.isSuccessful()) {
                            String rawMessage = extractHttpError(response);
                            result.postValue(NetworkResultWrapper.error(
                                    toFriendlyTransactionError(rawMessage, response.code()),
                                    response.code()
                            ));
                            return;
                        }
                        BaseResponse<CreateInboundSubCategoryResponseDto> body = response.body();
                        if (body == null || !body.isSuccess()) {
                            result.postValue(NetworkResultWrapper.error(toFriendlyTransactionError(
                                    body != null ? body.getMessage() : null,
                                    0
                            )));
                            return;
                        }
                        result.postValue(NetworkResultWrapper.success(mapCreatedSubCategory(body.getData())));
                    }

                    @Override
                    public void onFailure(Call<BaseResponse<CreateInboundSubCategoryResponseDto>> call,
                                          Throwable throwable) {
                        Log.e(TAG, "Inbound sub-category create failed", throwable);
                        result.postValue(NetworkResultWrapper.error(NETWORK_ERROR));
                    }
                });

        return result;
    }

    @Override
    public LiveData<NetworkResultWrapper<InventoryTransactionResult>> confirmInbound(String donationId,
                                                                                    String code,
                                                                                    List<InboundDraftItem> items,
                                                                                    @Nullable String generalNote) {
        MutableLiveData<NetworkResultWrapper<InventoryTransactionResult>> result = new MutableLiveData<>();
        result.postValue(NetworkResultWrapper.loading());

        apiService.confirmInbound(toInboundConfirmRequest(donationId, code, items, generalNote))
                .enqueue(new Callback<BaseResponse<InventoryTransactionResponseDto>>() {
                    @Override
                    public void onResponse(Call<BaseResponse<InventoryTransactionResponseDto>> call,
                                           Response<BaseResponse<InventoryTransactionResponseDto>> response) {
                        handleTransactionResponse(response, result);
                    }

                    @Override
                    public void onFailure(Call<BaseResponse<InventoryTransactionResponseDto>> call,
                                          Throwable throwable) {
                        Log.e(TAG, "Inbound confirm failed", throwable);
                        result.postValue(NetworkResultWrapper.error(NETWORK_ERROR));
                    }
                });

        return result;
    }

    @Override
    public LiveData<NetworkResultWrapper<InventoryQrPreview>> previewOutbound(String code) {
        MutableLiveData<NetworkResultWrapper<InventoryQrPreview>> result = new MutableLiveData<>();
        result.postValue(NetworkResultWrapper.loading());

        apiService.previewOutbound(trimToNull(code)).enqueue(new Callback<BaseResponse<InventoryQrPreviewResponseDto>>() {
            @Override
            public void onResponse(Call<BaseResponse<InventoryQrPreviewResponseDto>> call,
                                   Response<BaseResponse<InventoryQrPreviewResponseDto>> response) {
                handlePreviewResponse(response, result);
            }

            @Override
            public void onFailure(Call<BaseResponse<InventoryQrPreviewResponseDto>> call, Throwable throwable) {
                Log.e(TAG, "Outbound preview failed", throwable);
                result.postValue(NetworkResultWrapper.error(NETWORK_ERROR));
            }
        });

        return result;
    }

    @Override
    public LiveData<NetworkResultWrapper<InventoryTransactionResult>> confirmOutbound(String code,
                                                                                     List<InventoryConfirmItem> items,
                                                                                     @Nullable String note) {
        MutableLiveData<NetworkResultWrapper<InventoryTransactionResult>> result = new MutableLiveData<>();
        result.postValue(NetworkResultWrapper.loading());

        apiService.confirmOutbound(toConfirmRequest(code, items, note))
                .enqueue(new Callback<BaseResponse<InventoryTransactionResponseDto>>() {
                    @Override
                    public void onResponse(Call<BaseResponse<InventoryTransactionResponseDto>> call,
                                           Response<BaseResponse<InventoryTransactionResponseDto>> response) {
                        handleTransactionResponse(response, result);
                    }

                    @Override
                    public void onFailure(Call<BaseResponse<InventoryTransactionResponseDto>> call,
                                          Throwable throwable) {
                        Log.e(TAG, "Outbound confirm failed", throwable);
                        result.postValue(NetworkResultWrapper.error(NETWORK_ERROR));
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

    private void handlePreviewResponse(Response<BaseResponse<InventoryQrPreviewResponseDto>> response,
                                       MutableLiveData<NetworkResultWrapper<InventoryQrPreview>> result) {
        if (!response.isSuccessful()) {
            String rawMessage = extractHttpError(response);
            Log.w(TAG, "Inventory preview failed: " + rawMessage);
            result.postValue(NetworkResultWrapper.error(
                    toFriendlyTransactionError(rawMessage, response.code()),
                    response.code()
            ));
            return;
        }

        BaseResponse<InventoryQrPreviewResponseDto> body = response.body();
        if (body == null) {
            result.postValue(NetworkResultWrapper.error(GENERIC_TRANSACTION_ERROR));
            return;
        }

        if (!body.isSuccess()) {
            String rawMessage = body.getMessage();
            Log.w(TAG, "Inventory preview API returned error: " + rawMessage);
            result.postValue(NetworkResultWrapper.error(toFriendlyTransactionError(rawMessage, 0)));
            return;
        }

        result.postValue(NetworkResultWrapper.success(mapPreviewToDomain(body.getData())));
    }

    private void handleInboundPreviewResponse(Response<BaseResponse<InboundDonationPreviewResponseDto>> response,
                                              MutableLiveData<NetworkResultWrapper<InboundDonationPreview>> result) {
        if (!response.isSuccessful()) {
            String rawMessage = extractHttpError(response);
            Log.w(TAG, "Inbound preview failed: " + rawMessage);
            result.postValue(NetworkResultWrapper.error(
                    toFriendlyTransactionError(rawMessage, response.code()),
                    response.code()
            ));
            return;
        }

        BaseResponse<InboundDonationPreviewResponseDto> body = response.body();
        if (body == null) {
            result.postValue(NetworkResultWrapper.error(GENERIC_TRANSACTION_ERROR));
            return;
        }

        if (!body.isSuccess()) {
            String rawMessage = body.getMessage();
            Log.w(TAG, "Inbound preview API returned error: " + rawMessage);
            result.postValue(NetworkResultWrapper.error(toFriendlyTransactionError(rawMessage, 0)));
            return;
        }

        result.postValue(NetworkResultWrapper.success(mapInboundPreviewToDomain(body.getData())));
    }

    private void handleTransactionResponse(Response<BaseResponse<InventoryTransactionResponseDto>> response,
                                           MutableLiveData<NetworkResultWrapper<InventoryTransactionResult>> result) {
        if (!response.isSuccessful()) {
            String rawMessage = extractHttpError(response);
            Log.w(TAG, "Inventory transaction failed: " + rawMessage);
            result.postValue(NetworkResultWrapper.error(
                    toFriendlyTransactionError(rawMessage, response.code()),
                    response.code()
            ));
            return;
        }

        BaseResponse<InventoryTransactionResponseDto> body = response.body();
        if (body == null) {
            result.postValue(NetworkResultWrapper.error(GENERIC_TRANSACTION_ERROR));
            return;
        }

        if (!body.isSuccess()) {
            String rawMessage = body.getMessage();
            Log.w(TAG, "Inventory transaction API returned error: " + rawMessage);
            result.postValue(NetworkResultWrapper.error(toFriendlyTransactionError(rawMessage, 0)));
            return;
        }

        result.postValue(NetworkResultWrapper.success(mapTransactionToDomain(body.getData())));
    }

    private ConfirmInventoryRequestDto toConfirmRequest(String code,
                                                        List<InventoryConfirmItem> items,
                                                        @Nullable String note) {
        List<ConfirmInventoryItemRequestDto> requestItems = new ArrayList<>();
        if (items != null) {
            for (InventoryConfirmItem item : items) {
                if (item == null || item.getItemCategoryId().isEmpty()) {
                    continue;
                }
                requestItems.add(new ConfirmInventoryItemRequestDto(
                        item.getItemCategoryId(),
                        item.getQuantity()
                ));
            }
        }
        return new ConfirmInventoryRequestDto(trimToNull(code), requestItems, trimToNull(note));
    }

    private ConfirmInboundInventoryRequestDto toInboundConfirmRequest(String donationId,
                                                                      String code,
                                                                      List<InboundDraftItem> items,
                                                                      @Nullable String generalNote) {
        List<ConfirmInboundInventoryItemRequestDto> requestItems = new ArrayList<>();
        if (items != null) {
            for (InboundDraftItem item : items) {
                if (item == null
                        || item.getParentCategoryId().isEmpty()
                        || item.getItemCategoryId().isEmpty()) {
                    continue;
                }
                requestItems.add(new ConfirmInboundInventoryItemRequestDto(
                        item.getParentCategoryId(),
                        item.getItemCategoryId(),
                        item.getQuantity(),
                        trimToNull(item.getNote())
                ));
            }
        }
        return new ConfirmInboundInventoryRequestDto(
                trimToNull(donationId),
                trimToNull(code),
                requestItems,
                trimToNull(generalNote)
        );
    }

    private InboundDonationPreview mapInboundPreviewToDomain(@Nullable InboundDonationPreviewResponseDto dto) {
        if (dto == null) {
            return new InboundDonationPreview("", "", "", "", "", new ArrayList<>());
        }

        String hubName = dto.getHub() != null ? dto.getHub().getName() : "";
        return new InboundDonationPreview(
                dto.getDonationId(),
                dto.getDonationCode(),
                dto.getStatus(),
                hubName,
                dto.getMessage(),
                mapInboundParents(dto.getRegisteredParentCategories())
        );
    }

    private List<InboundParentCategory> mapInboundParents(
            @Nullable List<InboundDonationPreviewResponseDto.ParentCategoryDto> dtos) {
        List<InboundParentCategory> parents = new ArrayList<>();
        if (dtos == null) {
            return parents;
        }
        for (InboundDonationPreviewResponseDto.ParentCategoryDto dto : dtos) {
            if (dto == null) {
                continue;
            }
            parents.add(new InboundParentCategory(
                    dto.getParentCategoryId(),
                    dto.getParentCategoryName(),
                    dto.getUnit(),
                    mapInboundSubCategories(dto.getAvailableSubCategories())
            ));
        }
        return parents;
    }

    private List<InboundSubCategory> mapInboundSubCategories(@Nullable List<InboundSubCategoryDto> dtos) {
        List<InboundSubCategory> items = new ArrayList<>();
        if (dtos == null) {
            return items;
        }
        for (InboundSubCategoryDto dto : dtos) {
            if (dto == null) {
                continue;
            }
            items.add(new InboundSubCategory(
                    dto.getItemCategoryId(),
                    dto.getName(),
                    dto.getUnit()
            ));
        }
        return items;
    }

    private InboundSubCategory mapCreatedSubCategory(@Nullable CreateInboundSubCategoryResponseDto dto) {
        if (dto == null) {
            return new InboundSubCategory("", "", "");
        }
        return new InboundSubCategory(dto.getItemCategoryId(), dto.getName(), dto.getUnit());
    }

    private InventoryQrPreview mapPreviewToDomain(@Nullable InventoryQrPreviewResponseDto dto) {
        if (dto == null) {
            return new InventoryQrPreview(
                    "", "", "", "", "", "", "", "",
                    new ArrayList<>(), false, null, ""
            );
        }

        return new InventoryQrPreview(
                dto.getType(),
                dto.getDonationId(),
                dto.getDonationCode(),
                dto.getMissionId(),
                dto.getMissionCode(),
                dto.getStatus(),
                dto.getHubId(),
                dto.getHubName(),
                mapPreviewItems(dto.getItems()),
                dto.getCanConfirm() != null && dto.getCanConfirm(),
                mapAidRequestDetail(dto.getAidRequestDetail()),
                dto.getMessage()
        );
    }

    private OutboundAidRequestDetail mapAidRequestDetail(@Nullable OutboundAidRequestDetailDto dto) {
        if (dto == null) {
            return null;
        }
        return new OutboundAidRequestDetail(
                dto.getId(),
                dto.getDescription(),
                safeInt(dto.getNumberAdult()),
                safeInt(dto.getNumberElderly()),
                safeInt(dto.getNumberChildren()),
                mapAidRequestItems(dto.getItems())
        );
    }

    private List<OutboundAidRequestItem> mapAidRequestItems(@Nullable List<OutboundAidRequestItemDto> dtos) {
        List<OutboundAidRequestItem> items = new ArrayList<>();
        if (dtos == null) {
            return items;
        }
        for (OutboundAidRequestItemDto dto : dtos) {
            if (dto == null) {
                continue;
            }
            items.add(new OutboundAidRequestItem(
                    dto.getItemCategoryId(),
                    dto.getName(),
                    dto.getUnit(),
                    safeInt(dto.getRequestedQuantity())
            ));
        }
        return items;
    }

    private List<InventoryQrPreviewItem> mapPreviewItems(@Nullable List<InventoryQrPreviewItemDto> dtos) {
        List<InventoryQrPreviewItem> items = new ArrayList<>();
        if (dtos == null) {
            return items;
        }

        for (InventoryQrPreviewItemDto dto : dtos) {
            if (dto == null) {
                continue;
            }
            int quantity = safeInt(dto.getQuantity());
            int requiredQuantity = safeInt(dto.getRequiredQuantity());
            items.add(new InventoryQrPreviewItem(
                    dto.getItemCategoryId(),
                    dto.getName(),
                    dto.getUnit(),
                    dto.getParentCategoryName(),
                    quantity,
                    requiredQuantity,
                    safeInt(dto.getCurrentQuantity()),
                    dto.isEnoughStock() == null || dto.isEnoughStock()
            ));
        }
        return items;
    }

    private InventoryTransactionResult mapTransactionToDomain(@Nullable InventoryTransactionResponseDto dto) {
        if (dto == null) {
            return new InventoryTransactionResult("", "", "", "", new ArrayList<>());
        }

        return new InventoryTransactionResult(
                dto.getMessage(),
                dto.getDonationId(),
                dto.getDonationCode(),
                dto.getMissionId(),
                mapTransactionItems(dto.getUpdatedItems())
        );
    }

    private List<InventoryTransactionItem> mapTransactionItems(@Nullable List<InventoryTransactionItemDto> dtos) {
        List<InventoryTransactionItem> items = new ArrayList<>();
        if (dtos == null) {
            return items;
        }

        for (InventoryTransactionItemDto dto : dtos) {
            if (dto == null) {
                continue;
            }
            items.add(new InventoryTransactionItem(
                    dto.getItemCategoryId(),
                    dto.getName(),
                    dto.getQuantityDelta() != null ? dto.getQuantityDelta() : 0,
                    safeInt(dto.getQuantityAfter())
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

    private String toFriendlyTransactionError(@Nullable String rawMessage, int code) {
        String safe = rawMessage != null ? rawMessage.trim() : "";
        if (safe.contains(STAFF_UNASSIGNED_ERROR)) {
            return STAFF_UNASSIGNED_ERROR;
        }
        if (looksLikeRawServerError(safe)) {
            return GENERIC_TRANSACTION_ERROR;
        }
        if (code == 404) {
            return "Kh\u00f4ng t\u00ecm th\u1ea5y m\u00e3.";
        }
        if (code == 403) {
            return "M\u00e3 kh\u00f4ng thu\u1ed9c hub c\u1ee7a b\u1ea1n ho\u1eb7c b\u1ea1n kh\u00f4ng c\u00f3 quy\u1ec1n thao t\u00e1c.";
        }
        if (code == 409 && safe.isEmpty()) {
            return "Giao d\u1ecbch kh\u00f4ng h\u1ee3p l\u1ec7 ho\u1eb7c t\u1ed3n kho kh\u00f4ng \u0111\u1ee7.";
        }
        return safe.isEmpty() ? GENERIC_TRANSACTION_ERROR : safe;
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
