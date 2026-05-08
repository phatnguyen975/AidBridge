package com.drc.aidbridge.ui.main.viewmodel.staff;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.domain.model.staff.StaffUpcomingTask;
import com.drc.aidbridge.domain.usecase.staff.GetStaffUpcomingDeliveryMissionsUseCase;
import com.drc.aidbridge.domain.usecase.staff.GetStaffUpcomingDonationsUseCase;
import com.drc.aidbridge.ui.base.BaseViewModel;

import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class StaffTasksViewModel extends BaseViewModel {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_LIMIT = 50;

    private final MutableLiveData<QueryParams> queryParams = new MutableLiveData<>();
    private final LiveData<NetworkResultWrapper<List<StaffUpcomingTask>>> tasksResult;

    private final GetStaffUpcomingDeliveryMissionsUseCase getDeliveriesUseCase;
    private final GetStaffUpcomingDonationsUseCase getDonationsUseCase;

    @Inject
    public StaffTasksViewModel(GetStaffUpcomingDeliveryMissionsUseCase getDeliveriesUseCase,
                               GetStaffUpcomingDonationsUseCase getDonationsUseCase) {
        this.getDeliveriesUseCase = getDeliveriesUseCase;
        this.getDonationsUseCase = getDonationsUseCase;

        tasksResult = Transformations.switchMap(queryParams, params -> {
            if (params == null || params.tab == Tab.DELIVERY) {
                return this.getDeliveriesUseCase.execute(DEFAULT_PAGE, DEFAULT_LIMIT);
            }
            return this.getDonationsUseCase.execute(DEFAULT_PAGE, DEFAULT_LIMIT);
        });

        loadDeliveries();
    }

    public LiveData<NetworkResultWrapper<List<StaffUpcomingTask>>> getTasksResult() {
        return tasksResult;
    }

    public void loadDeliveries() {
        queryParams.setValue(new QueryParams(Tab.DELIVERY));
    }

    public void loadDonations() {
        queryParams.setValue(new QueryParams(Tab.DONATION));
    }

    private enum Tab {
        DELIVERY,
        DONATION
    }

    private static final class QueryParams {
        final Tab tab;

        QueryParams(Tab tab) {
            this.tab = tab;
        }
    }
}
