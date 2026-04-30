package com.drc.aidbridge.domain.usecase.admin;

import androidx.lifecycle.LiveData;

import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.domain.model.admin.Staff;
import com.drc.aidbridge.domain.repository.admin.AdminStaffRepository;

import java.util.List;

import javax.inject.Inject;

public class ListStaffUseCase {

    private final AdminStaffRepository repository;

    @Inject
    public ListStaffUseCase(AdminStaffRepository repository) {
        this.repository = repository;
    }

    public LiveData<NetworkResultWrapper<List<Staff>>> execute() {
        return repository.getStaffList();
    }
}
