package com.drc.aidbridge.data.remote.api.staff;

import com.drc.aidbridge.data.remote.dto.response.BaseResponse;
import com.drc.aidbridge.data.remote.dto.request.staff.ConfirmInventoryRequestDto;
import com.drc.aidbridge.data.remote.dto.request.staff.ConfirmInboundInventoryRequestDto;
import com.drc.aidbridge.data.remote.dto.request.staff.CreateInboundSubCategoryRequestDto;
import com.drc.aidbridge.data.remote.dto.response.staff.CreateInboundSubCategoryResponseDto;
import com.drc.aidbridge.data.remote.dto.response.staff.InboundDonationPreviewResponseDto;
import com.drc.aidbridge.data.remote.dto.response.staff.InventoryQrPreviewResponseDto;
import com.drc.aidbridge.data.remote.dto.response.staff.InventoryTransactionResponseDto;
import com.drc.aidbridge.data.remote.dto.response.staff.SearchInboundSubCategoriesResponseDto;
import com.drc.aidbridge.data.remote.dto.response.staff.StaffInventoryResponseDto;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Body;
import retrofit2.http.Query;

public interface StaffInventoryApiService {

    @GET("staff/inventory")
    Call<BaseResponse<StaffInventoryResponseDto>> getMyHubInventory(
            @Query("parentCategoryId") String parentCategoryId,
            @Query("parentCategoryName") String parentCategoryName,
            @Query("keyword") String keyword,
            @Query("page") int page,
            @Query("size") int size
    );

    @GET("staff/inventory/inbound/preview")
    Call<BaseResponse<InboundDonationPreviewResponseDto>> previewInbound(
            @Query("code") String code
    );

    @GET("staff/inventory/inbound/sub-categories/search")
    Call<BaseResponse<SearchInboundSubCategoriesResponseDto>> searchInboundSubCategories(
            @Query("donationId") String donationId,
            @Query("parentCategoryId") String parentCategoryId,
            @Query("keyword") String keyword
    );

    @POST("staff/inventory/inbound/sub-categories")
    Call<BaseResponse<CreateInboundSubCategoryResponseDto>> createInboundSubCategory(
            @Body CreateInboundSubCategoryRequestDto request
    );

    @POST("staff/inventory/inbound/confirm")
    Call<BaseResponse<InventoryTransactionResponseDto>> confirmInbound(
            @Body ConfirmInboundInventoryRequestDto request
    );

    @GET("staff/inventory/outbound/preview")
    Call<BaseResponse<InventoryQrPreviewResponseDto>> previewOutbound(
            @Query("code") String code
    );

    @POST("staff/inventory/outbound/confirm")
    Call<BaseResponse<InventoryTransactionResponseDto>> confirmOutbound(
            @Body ConfirmInventoryRequestDto request
    );
}
