package com.drc.aidbridge.domain.repository.staff;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;

import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.domain.model.staff.StaffInventory;

public interface StaffInventoryRepository {

    LiveData<NetworkResultWrapper<StaffInventory>> getMyHubInventory(@Nullable String parentCategoryId,
                                                                     @Nullable String parentCategoryName,
                                                                     @Nullable String keyword,
                                                                     int page,
                                                                     int size);
}
