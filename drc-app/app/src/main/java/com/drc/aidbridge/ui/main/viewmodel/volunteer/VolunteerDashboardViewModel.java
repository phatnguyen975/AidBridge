package com.drc.aidbridge.ui.main.viewmodel.volunteer;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.data.remote.dto.request.volunteer.ToggleStatusRequest;
import com.drc.aidbridge.domain.model.volunteer.VolunteerDashboardInfo;
import com.drc.aidbridge.domain.usecase.volunteer.GetVolunteerDashboardInfoUseCase;
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
    private final MediatorLiveData<NetworkResultWrapper<VolunteerDashboardInfo>> volunteerDashboardInfoResult = new MediatorLiveData<>();
    private final LiveData<NetworkResultWrapper<Boolean>> toggleStatusResult;

    private VolunteerDashboardInfo currentDashboardInfo;
    private Boolean onlineOverride;

    @Inject
    public VolunteerDashboardViewModel(GetVolunteerDashboardInfoUseCase getVolunteerDashboardInfoUseCase,
            ToggleVolunteerStatusUseCase toggleVolunteerStatusUseCase) {
        this.toggleStatusResult = initLiveData(getVolunteerDashboardInfoUseCase, toggleVolunteerStatusUseCase);
    }

    private LiveData<NetworkResultWrapper<Boolean>> initLiveData(
            GetVolunteerDashboardInfoUseCase getVolunteerDashboardInfoUseCase,
            ToggleVolunteerStatusUseCase toggleVolunteerStatusUseCase) {
        LiveData<NetworkResultWrapper<VolunteerDashboardInfo>> profileSource = Transformations.switchMap(
                profileTrigger,
                trigger -> getVolunteerDashboardInfoUseCase.execute());

        volunteerDashboardInfoResult.addSource(profileSource, this::handleProfileResult);

        return Transformations.switchMap(
                toggleTrigger,
                request -> Transformations.map(
                        toggleVolunteerStatusUseCase.execute(request),
                        this::handleToggleResult));
    }

    private void handleProfileResult(NetworkResultWrapper<VolunteerDashboardInfo> result) {
        if (result == null) {
            return;
        }

        if (result.isSuccess() && result.getData() != null) {
            VolunteerDashboardInfo latest = applyOnlineOverride(result.getData());
            currentDashboardInfo = latest;
            volunteerDashboardInfoResult.setValue(NetworkResultWrapper.success(latest));
            return;
        }

        volunteerDashboardInfoResult.setValue(result);
    }

    private NetworkResultWrapper<Boolean> handleToggleResult(NetworkResultWrapper<Boolean> result) {
        if (result != null && result.isSuccess() && result.getData() != null) {
            boolean newOnlineStatus = result.getData();
            onlineOverride = newOnlineStatus;
            publishOnlineStatusToDashboard(newOnlineStatus);
        }
        return result;
    }

    private VolunteerDashboardInfo applyOnlineOverride(VolunteerDashboardInfo source) {
        if (onlineOverride == null) {
            return source;
        }
        return new VolunteerDashboardInfo(
                source.getFullName(),
                onlineOverride,
                source.getTotalCompletedTasks());
    }

    private void publishOnlineStatusToDashboard(boolean isOnline) {
        if (currentDashboardInfo == null) {
            return;
        }

        currentDashboardInfo = new VolunteerDashboardInfo(
                currentDashboardInfo.getFullName(),
                isOnline,
                currentDashboardInfo.getTotalCompletedTasks());
        volunteerDashboardInfoResult.setValue(NetworkResultWrapper.success(currentDashboardInfo));
    }

    public LiveData<NetworkResultWrapper<VolunteerDashboardInfo>> getVolunteerDashboardInfoResult() {
        return volunteerDashboardInfoResult;
    }

    public LiveData<NetworkResultWrapper<Boolean>> getToggleStatusResult() {
        return toggleStatusResult;
    }

    public void loadProfileDashboard() {
        profileTrigger.setValue(Boolean.TRUE);
    }

    public void toggleStatus(boolean isOnline) {
        // Optimistic update: update source of truth first so UI never jumps backward.
        onlineOverride = isOnline;
        publishOnlineStatusToDashboard(isOnline);
        toggleTrigger.setValue(new ToggleStatusRequest(isOnline, null, null));
    }
}
