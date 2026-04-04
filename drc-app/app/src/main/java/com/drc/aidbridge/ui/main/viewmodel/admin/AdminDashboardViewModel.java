package com.drc.aidbridge.ui.main.viewmodel.admin;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.ui.base.BaseViewModel;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

/**
 * ViewModel for Admin Dashboard screen.
 * Currently UI-only and reserved for future backend/use case integration.
 */
@HiltViewModel
public class AdminDashboardViewModel extends BaseViewModel {

    private final MutableLiveData<String> inventoryTrigger = new MutableLiveData<>();
    private final LiveData<NetworkResultWrapper<float[]>> inventoryData;

    @Inject
    public AdminDashboardViewModel() {
        this.inventoryData = Transformations.switchMap(inventoryTrigger, trigger -> {
            MutableLiveData<NetworkResultWrapper<float[]>> mockResult = new MutableLiveData<>();
            /*
             * TODO: Tích hợp Repository thực tế để lấy dữ liệu tồn kho theo thời gian thực.
             */
            mockResult.setValue(NetworkResultWrapper.success(new float[] { 1200f, 850f, 430f, 1300f }));
            return mockResult;
        });

        /*
         * TODO: Tích hợp API Backend và UseCase để xử lý logic dữ liệu tại đây
         */
    }

    public LiveData<NetworkResultWrapper<float[]>> getInventoryData() {
        return inventoryData;
    }

    public void loadInventoryData() {
        inventoryTrigger.setValue(String.valueOf(System.currentTimeMillis()));
    }
}