package com.drc.aidbridge.ui.main.viewmodel.staff;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.domain.model.staff.StaffInventory;
import com.drc.aidbridge.domain.usecase.staff.GetStaffInventoryUseCase;
import com.drc.aidbridge.ui.base.BaseViewModel;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class StaffInventoryViewModel extends BaseViewModel {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 100;

    private final MutableLiveData<QueryParams> queryParams = new MutableLiveData<>();
    private final LiveData<NetworkResultWrapper<StaffInventory>> inventoryResult;

    private String selectedParentCategoryId;
    private String selectedParentCategoryName;
    private String keyword = "";

    @Inject
    public StaffInventoryViewModel(GetStaffInventoryUseCase getStaffInventoryUseCase) {
        inventoryResult = Transformations.switchMap(
                queryParams,
                params -> getStaffInventoryUseCase.execute(
                        params.parentCategoryId,
                        params.parentCategoryName,
                        params.keyword,
                        params.page,
                        params.size
                )
        );
        refresh();
    }

    public LiveData<NetworkResultWrapper<StaffInventory>> getInventoryResult() {
        return inventoryResult;
    }

    public void selectFilter(@Nullable String parentCategoryId,
                             @Nullable String parentCategoryName) {
        selectedParentCategoryId = trimToNull(parentCategoryId);
        selectedParentCategoryName = trimToNull(parentCategoryName);
        refresh();
    }

    public void updateKeyword(@Nullable String newKeyword) {
        String normalized = newKeyword != null ? newKeyword.trim() : "";
        if (normalized.equals(keyword)) {
            return;
        }
        keyword = normalized;
        refresh();
    }

    public void refresh() {
        queryParams.setValue(new QueryParams(
                selectedParentCategoryId,
                selectedParentCategoryName,
                keyword,
                DEFAULT_PAGE,
                DEFAULT_SIZE
        ));
    }

    private String trimToNull(@Nullable String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static final class QueryParams {
        final String parentCategoryId;
        final String parentCategoryName;
        final String keyword;
        final int page;
        final int size;

        QueryParams(String parentCategoryId,
                    String parentCategoryName,
                    String keyword,
                    int page,
                    int size) {
            this.parentCategoryId = parentCategoryId;
            this.parentCategoryName = parentCategoryName;
            this.keyword = keyword;
            this.page = page;
            this.size = size;
        }
    }
}
