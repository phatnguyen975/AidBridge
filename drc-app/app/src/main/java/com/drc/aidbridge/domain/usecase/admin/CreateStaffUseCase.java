package com.drc.aidbridge.domain.usecase.admin;

import androidx.lifecycle.LiveData;

import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.domain.model.admin.Staff;
import com.drc.aidbridge.domain.repository.admin.AdminStaffRepository;

import javax.inject.Inject;

public class CreateStaffUseCase {

    private final AdminStaffRepository repository;

    @Inject
    public CreateStaffUseCase(AdminStaffRepository repository) {
        this.repository = repository;
    }

    public LiveData<NetworkResultWrapper<Staff>> execute(String fullName,
                                                         String email,
                                                         String phoneNumber,
                                                         String password,
                                                         String hubId) {
        return repository.createStaff(fullName, email, phoneNumber, password, hubId);
    }
}
