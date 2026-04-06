package com.drc.aidbridge.ui.main.viewmodel.volunteer;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.data.remote.dto.request.volunteer.ToggleStatusRequest;
import com.drc.aidbridge.domain.model.volunteer.VolunteerDashboardInfo;
import com.drc.aidbridge.domain.model.volunteer.VolunteerPersonalInfo;
import com.drc.aidbridge.domain.usecase.volunteer.GetVolunteerDashboardInfoUseCase;
import com.drc.aidbridge.domain.usecase.volunteer.GetVolunteerPersonalInfoUseCase;
import com.drc.aidbridge.domain.usecase.volunteer.ToggleVolunteerStatusUseCase;
import com.drc.aidbridge.ui.base.BaseViewModel;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

/**
 * VolunteerDashboardViewModel — handles volunteer dashboard profile loading.
 */
@HiltViewModel
public class VolunteerDashboardViewModel extends BaseViewModel {

    private final MutableLiveData<Boolean> profileTrigger = new MutableLiveData<>();
    private final MutableLiveData<ToggleStatusRequest> toggleTrigger = new MutableLiveData<>();
    private LiveData<NetworkResultWrapper<VolunteerDashboardInfo>> volunteerDashboardInfoResult;
    private LiveData<NetworkResultWrapper<VolunteerPersonalInfo>> volunteerPersonalInfoResult;
    private LiveData<NetworkResultWrapper<Boolean>> toggleStatusResult;

    @Inject
    public VolunteerDashboardViewModel(GetVolunteerDashboardInfoUseCase getVolunteerDashboardInfoUseCase,
            GetVolunteerPersonalInfoUseCase getVolunteerPersonalInfoUseCase,
            ToggleVolunteerStatusUseCase toggleVolunteerStatusUseCase) {
        initDashboardStreams(getVolunteerDashboardInfoUseCase);
        initPersonalInfoStreams(getVolunteerPersonalInfoUseCase);
        initToggleStreams(toggleVolunteerStatusUseCase);
    }

    private void initDashboardStreams(GetVolunteerDashboardInfoUseCase getVolunteerDashboardInfoUseCase) {
        volunteerDashboardInfoResult = Transformations.switchMap(
                profileTrigger,
                trigger -> getVolunteerDashboardInfoUseCase.execute());
    }

    private void initPersonalInfoStreams(GetVolunteerPersonalInfoUseCase getVolunteerPersonalInfoUseCase) {
        volunteerPersonalInfoResult = Transformations.switchMap(
                profileTrigger,
                trigger -> getVolunteerPersonalInfoUseCase.execute());
    }

    private void initToggleStreams(ToggleVolunteerStatusUseCase toggleVolunteerStatusUseCase) {
        toggleStatusResult = Transformations.switchMap(
                toggleTrigger,
                toggleVolunteerStatusUseCase::execute);
    }

    public LiveData<NetworkResultWrapper<VolunteerDashboardInfo>> getVolunteerDashboardInfoResult() {
        return volunteerDashboardInfoResult;
    }

    public LiveData<NetworkResultWrapper<VolunteerPersonalInfo>> getVolunteerPersonalInfoResult() {
        return volunteerPersonalInfoResult;
    }

    public LiveData<NetworkResultWrapper<Boolean>> getToggleStatusResult() {
        return toggleStatusResult;
    }

    public void loadProfileDashboard() {
        profileTrigger.setValue(Boolean.TRUE);
    }

    public void toggleStatus(boolean isOnline) {
        toggleTrigger.setValue(new ToggleStatusRequest(isOnline, null, null));
    }
}
