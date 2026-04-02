package com.drc.aidbridge.ui.main.viewmodel.admin;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.drc.aidbridge.R;
import com.drc.aidbridge.ui.base.BaseViewModel;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class AdminHubManagementViewModel extends BaseViewModel {

    private final MutableLiveData<List<Hub>> hubs = new MutableLiveData<>(new ArrayList<>());
    private final List<Hub> mockHubStore = new ArrayList<>();

    @Inject
    public AdminHubManagementViewModel() {
        // TODO: Tích hợp API Backend qua UseCase để lấy danh sách trạm thực tế và thực
        // hiện cập nhật trạng thái trạm (Active/Inactive)
    }

    public LiveData<List<Hub>> getHubs() {
        return hubs;
    }

    public void loadMockHubs() {
        mockHubStore.clear();
        mockHubStore.add(new Hub(1L, R.string.admin_hub_mock_name_q1, R.string.admin_hub_mock_address_q1,
                R.string.admin_hub_mock_inventory_q1, true));
        mockHubStore.add(new Hub(2L, R.string.admin_hub_mock_name_q7, R.string.admin_hub_mock_address_q7,
                R.string.admin_hub_mock_inventory_q7, true));
        mockHubStore.add(new Hub(3L, R.string.admin_hub_mock_name_thu_duc, R.string.admin_hub_mock_address_thu_duc,
                R.string.admin_hub_mock_inventory_thu_duc, false));
        mockHubStore
                .add(new Hub(4L, R.string.admin_hub_mock_name_binh_thanh, R.string.admin_hub_mock_address_binh_thanh,
                        R.string.admin_hub_mock_inventory_binh_thanh, true));
        hubs.setValue(new ArrayList<>(mockHubStore));
    }

    public void toggleHubStatus(long hubId) {
        for (int i = 0; i < mockHubStore.size(); i++) {
            Hub current = mockHubStore.get(i);
            if (current.id == hubId) {
                mockHubStore.set(i, new Hub(current.id, current.nameResId, current.addressResId,
                        current.inventoryResId, !current.isActive));
                break;
            }
        }
        hubs.setValue(new ArrayList<>(mockHubStore));
    }

    public static class Hub {
        public final long id;
        @StringRes
        public final int nameResId;
        @StringRes
        public final int addressResId;
        @StringRes
        public final int inventoryResId;
        public final boolean isActive;

        public Hub(long id,
                @StringRes int nameResId,
                @StringRes int addressResId,
                @StringRes int inventoryResId,
                boolean isActive) {
            this.id = id;
            this.nameResId = nameResId;
            this.addressResId = addressResId;
            this.inventoryResId = inventoryResId;
            this.isActive = isActive;
        }

        @NonNull
        public Hub withStatus(boolean active) {
            return new Hub(id, nameResId, addressResId, inventoryResId, active);
        }
    }
}