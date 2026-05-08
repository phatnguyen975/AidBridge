package com.drc.aidbridge.domain.repository.staff;

import androidx.lifecycle.LiveData;

import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.domain.model.staff.StaffUpcomingTask;

import java.util.List;

public interface StaffTasksRepository {

    LiveData<NetworkResultWrapper<List<StaffUpcomingTask>>> getUpcomingDonations(int page, int limit);

    LiveData<NetworkResultWrapper<List<StaffUpcomingTask>>> getUpcomingDeliveryMissions(int page, int limit);
}
