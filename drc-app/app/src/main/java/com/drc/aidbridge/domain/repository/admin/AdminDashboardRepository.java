package com.drc.aidbridge.domain.repository.admin;

import androidx.lifecycle.LiveData;

import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.data.remote.dto.response.admin.AdminRoutingSosAidResponseDto;
import com.drc.aidbridge.domain.model.admin.AdminDashboardSummary;

public interface AdminDashboardRepository {

    LiveData<NetworkResultWrapper<AdminDashboardSummary>> getSummary();

    LiveData<NetworkResultWrapper<AdminRoutingSosAidResponseDto>> getSosAidRequests(String status, String startDate, String endDate);
}
