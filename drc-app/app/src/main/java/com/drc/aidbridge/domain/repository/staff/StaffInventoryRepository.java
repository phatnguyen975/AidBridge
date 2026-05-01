package com.drc.aidbridge.domain.repository.staff;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;

import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.domain.model.staff.InventoryConfirmItem;
import com.drc.aidbridge.domain.model.staff.InventoryQrPreview;
import com.drc.aidbridge.domain.model.staff.InventoryTransactionResult;
import com.drc.aidbridge.domain.model.staff.InboundDonationPreview;
import com.drc.aidbridge.domain.model.staff.InboundDraftItem;
import com.drc.aidbridge.domain.model.staff.InboundSubCategory;
import com.drc.aidbridge.domain.model.staff.StaffInventory;

import java.util.List;

public interface StaffInventoryRepository {

    LiveData<NetworkResultWrapper<StaffInventory>> getMyHubInventory(@Nullable String parentCategoryId,
                                                                     @Nullable String parentCategoryName,
                                                                     @Nullable String keyword,
                                                                     int page,
                                                                     int size);

    LiveData<NetworkResultWrapper<InboundDonationPreview>> previewInbound(String code);

    LiveData<NetworkResultWrapper<List<InboundSubCategory>>> searchInboundSubCategories(String donationId,
                                                                                        String parentCategoryId,
                                                                                        @Nullable String keyword);

    LiveData<NetworkResultWrapper<InboundSubCategory>> createInboundSubCategory(String donationId,
                                                                                String parentCategoryId,
                                                                                String name,
                                                                                String unit,
                                                                                @Nullable String iconUrl);

    LiveData<NetworkResultWrapper<InventoryTransactionResult>> confirmInbound(String donationId,
                                                                              String code,
                                                                              List<InboundDraftItem> items,
                                                                              @Nullable String generalNote);

    LiveData<NetworkResultWrapper<InventoryQrPreview>> previewOutbound(String code);

    LiveData<NetworkResultWrapper<InventoryTransactionResult>> confirmOutbound(String code,
                                                                               List<InventoryConfirmItem> items,
                                                                               @Nullable String note);
}
