package com.drc.aidbridge.ui.main.viewmodel.admin;

import androidx.annotation.NonNull;

import com.drc.aidbridge.ui.base.BaseViewModel;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class AdminAddSupplyTypeViewModel extends BaseViewModel {

    private final Set<String> existingSupplyTypes = new HashSet<>();

    @Inject
    public AdminAddSupplyTypeViewModel() {
    }

    public void setExistingSupplyTypes(@NonNull List<String> categories) {
        existingSupplyTypes.clear();
        existingSupplyTypes.addAll(categories);
    }

    public boolean upsertSupplyType(@NonNull String category, int minLevel, @NonNull String note) {
        boolean isExisting = existingSupplyTypes.contains(category);
        existingSupplyTypes.add(category);

        // TODO: Tích hợp API Backend qua UseCase để kiểm tra sự tồn tại của loại vật tư
        // trong Hub và thực hiện lệnh POST/PUT (Atomic Update) vào Database thông qua
        // Repository
        return isExisting;
    }
}
