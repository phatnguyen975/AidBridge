package com.drc.aidbridge.domain.repository.admin;

import androidx.lifecycle.LiveData;

import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.domain.model.admin.Staff;

import java.util.List;

public interface AdminStaffRepository {

    LiveData<NetworkResultWrapper<List<Staff>>> getStaffList();

    LiveData<NetworkResultWrapper<Staff>> createStaff(String fullName,
                                                      String email,
                                                      String phoneNumber,
                                                      String password,
                                                      String hubId);
}
