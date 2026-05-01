package com.drc.aidbridge.ui.main.viewmodel.admin;

import android.os.Handler;
import android.os.Looper;

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
public class AdminAiSummaryViewModel extends BaseViewModel {

    private final GetAdminDashboardSummaryUseCase getSummaryUseCase;
    private final MutableLiveData<String> summaryTrigger = new MutableLiveData<>();
    private final MutableLiveData<Boolean> generatingReport = new MutableLiveData<>(false);
    private final LiveData<NetworkResultWrapper<AdminDashboardSummary>> dashboardSummary;

    @Inject
    public AdminAiSummaryViewModel(GetAdminDashboardSummaryUseCase getSummaryUseCase) {
        this.getSummaryUseCase = getSummaryUseCase;
        this.dashboardSummary = Transformations.switchMap(
                summaryTrigger,
                trigger -> this.getSummaryUseCase.execute()
        );
    }

    public LiveData<Boolean> getGeneratingReport() {
        return generatingReport;
    }

    public LiveData<NetworkResultWrapper<AdminDashboardSummary>> getDashboardSummary() {
        return dashboardSummary;
    }

    public void loadDashboardSummary() {
        summaryTrigger.setValue(String.valueOf(System.currentTimeMillis()));
    }

    public void generateReliefReport() {
        generatingReport.setValue(true);
        new Handler(Looper.getMainLooper()).postDelayed(
                () -> generatingReport.setValue(false),
                1500L
        );
    }
}