package com.drc.aidbridge.ui.main.viewmodel.admin;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.drc.aidbridge.ui.base.BaseViewModel;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class AdminAiSummaryViewModel extends BaseViewModel {

    private final MutableLiveData<Boolean> generatingReport = new MutableLiveData<>(false);

    @Inject
    public AdminAiSummaryViewModel() {
        // TODO: Tích hợp AI UseCase để lấy dữ liệu tổng hợp từ backend và tạo tóm tắt
        // báo cáo hàng ngày (Yêu cầu 6.5).
    }

    public LiveData<Boolean> getGeneratingReport() {
        return generatingReport;
    }

    public void generateReliefReport() {
        generatingReport.setValue(true);
        generatingReport.setValue(false);
    }
}