package com.drc.aidbridge.ui.main.viewmodel.volunteer;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.domain.model.volunteer.VolunteerDashboardInfo;
import com.drc.aidbridge.domain.usecase.volunteer.GetVolunteerDashboardInfoUseCase;
import com.drc.aidbridge.ui.base.BaseViewModel;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

/**
 * VolunteerDashboardViewModel — handles volunteer dashboard profile loading.
 */
@HiltViewModel
public class VolunteerDashboardViewModel extends BaseViewModel {

    private final MutableLiveData<Boolean> profileTrigger = new MutableLiveData<>();
    private final LiveData<NetworkResultWrapper<VolunteerDashboardInfo>> volunteerDashboardInfoResult;

    @Inject
    public VolunteerDashboardViewModel(GetVolunteerDashboardInfoUseCase getVolunteerDashboardInfoUseCase) {
        this.volunteerDashboardInfoResult = Transformations.switchMap(
                profileTrigger,
                trigger -> getVolunteerDashboardInfoUseCase.execute());
    }

    public LiveData<NetworkResultWrapper<VolunteerDashboardInfo>> getVolunteerDashboardInfoResult() {
        return volunteerDashboardInfoResult;
    }

    public void loadProfileDashboard() {
        profileTrigger.setValue(Boolean.TRUE);
    }
}
