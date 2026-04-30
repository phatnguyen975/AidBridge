package com.drc.aidbridge.domain.usecase.admin;

import androidx.lifecycle.LiveData;

import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.domain.model.admin.AdminDashboardSummary;
import com.drc.aidbridge.domain.repository.admin.AdminDashboardRepository;

import javax.inject.Inject;

public class GetAdminDashboardSummaryUseCase {

    private final AdminDashboardRepository repository;

    @Inject
    public GetAdminDashboardSummaryUseCase(AdminDashboardRepository repository) {
        this.repository = repository;
    }

    public LiveData<NetworkResultWrapper<AdminDashboardSummary>> execute() {
        return repository.getSummary();
    }
}
