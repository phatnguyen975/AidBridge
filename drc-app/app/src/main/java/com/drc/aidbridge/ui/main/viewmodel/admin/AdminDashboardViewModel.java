package com.drc.aidbridge.ui.main.viewmodel.admin;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.domain.model.admin.AdminDashboardSummary;
import com.drc.aidbridge.domain.usecase.admin.GetAdminDashboardSummaryUseCase;
import com.drc.aidbridge.ui.base.BaseViewModel;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class AdminDashboardViewModel extends BaseViewModel {

    private final MutableLiveData<String> summaryTrigger = new MutableLiveData<>();
    private final LiveData<NetworkResultWrapper<AdminDashboardSummary>> dashboardSummary;

    @Inject
    public AdminDashboardViewModel(GetAdminDashboardSummaryUseCase getAdminDashboardSummaryUseCase) {
        this.dashboardSummary = Transformations.switchMap(
                summaryTrigger,
                trigger -> getAdminDashboardSummaryUseCase.execute()
        );
    }

    public LiveData<NetworkResultWrapper<AdminDashboardSummary>> getDashboardSummary() {
        return dashboardSummary;
    }

    public void loadDashboardSummary() {
        summaryTrigger.setValue(String.valueOf(System.currentTimeMillis()));
    }
}
