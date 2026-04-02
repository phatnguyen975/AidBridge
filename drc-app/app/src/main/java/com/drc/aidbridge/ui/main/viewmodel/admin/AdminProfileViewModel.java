package com.drc.aidbridge.ui.main.viewmodel.admin;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.drc.aidbridge.ui.base.BaseViewModel;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class AdminProfileViewModel extends BaseViewModel {

    private final MutableLiveData<String> email = new MutableLiveData<>();
    private final MutableLiveData<String> phone = new MutableLiveData<>();

    @Inject
    public AdminProfileViewModel() {
        // TODO: Tích hợp API Backend để lấy thông tin hồ sơ Admin thực tế và xử lý các
        // lệnh phân quyền/cấu hình hệ thống (Yêu cầu 6.1, 6.2, 6.3)
    }

    public LiveData<String> getEmail() {
        return email;
    }

    public LiveData<String> getPhone() {
        return phone;
    }

    public void loadMockProfile() {
        email.setValue("admin@drc.gov.vn");
        phone.setValue("090xxxxxxx");
    }
}