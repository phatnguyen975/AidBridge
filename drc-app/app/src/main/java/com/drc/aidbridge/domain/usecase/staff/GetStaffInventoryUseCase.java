package com.drc.aidbridge.domain.usecase.staff;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;

import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.domain.model.staff.StaffInventory;
import com.drc.aidbridge.domain.repository.staff.StaffInventoryRepository;

import javax.inject.Inject;

public class GetStaffInventoryUseCase {

    private final StaffInventoryRepository repository;

    @Inject
    public GetStaffInventoryUseCase(StaffInventoryRepository repository) {
        this.repository = repository;
    }

    public LiveData<NetworkResultWrapper<StaffInventory>> execute(@Nullable String parentCategoryId,
                                                                  @Nullable String parentCategoryName,
                                                                  @Nullable String keyword,
                                                                  int page,
                                                                  int size) {
        return repository.getMyHubInventory(parentCategoryId, parentCategoryName, keyword, page, size);
    }
}
