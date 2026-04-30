package com.drc.aidbridge.ui.main.viewmodel.admin;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.domain.model.admin.Staff;
import com.drc.aidbridge.domain.usecase.admin.ListStaffUseCase;
import com.drc.aidbridge.ui.base.BaseViewModel;

import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class AdminStaffManagementViewModel extends BaseViewModel {

    private final MutableLiveData<Long> staffTrigger = new MutableLiveData<>();
    private final LiveData<NetworkResultWrapper<List<Staff>>> staffResult;

    @Inject
    public AdminStaffManagementViewModel(ListStaffUseCase listStaffUseCase) {
        staffResult = Transformations.switchMap(staffTrigger, ignored -> listStaffUseCase.execute());
    }

    public LiveData<NetworkResultWrapper<List<Staff>>> getStaffResult() {
        return staffResult;
    }

    public void fetchStaff() {
        staffTrigger.setValue(System.currentTimeMillis());
    }
}
