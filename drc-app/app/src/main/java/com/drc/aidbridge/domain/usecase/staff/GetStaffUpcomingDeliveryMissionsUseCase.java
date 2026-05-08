package com.drc.aidbridge.domain.usecase.staff;

import androidx.lifecycle.LiveData;

import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.domain.model.staff.StaffUpcomingTask;
import com.drc.aidbridge.domain.repository.staff.StaffTasksRepository;

import java.util.List;

import javax.inject.Inject;

public class GetStaffUpcomingDeliveryMissionsUseCase {

    private final StaffTasksRepository repository;

    @Inject
    public GetStaffUpcomingDeliveryMissionsUseCase(StaffTasksRepository repository) {
        this.repository = repository;
    }

    public LiveData<NetworkResultWrapper<List<StaffUpcomingTask>>> execute(int page, int limit) {
        return repository.getUpcomingDeliveryMissions(page, limit);
    }
}
