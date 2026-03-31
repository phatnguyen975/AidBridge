package com.drc.aidbridge.ui.main.viewmodel.volunteer;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.ui.base.BaseViewModel;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class VolunteerTaskViewModel extends BaseViewModel {

    private final MutableLiveData<Boolean> isMissionAccepted = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> isMissionIgnored = new MutableLiveData<>(false);
    private final MutableLiveData<String> currentMissionType = new MutableLiveData<>(null);
    private final MutableLiveData<Integer> currentDeliveryStep = new MutableLiveData<>(1);
    private final MutableLiveData<String> missionTrigger = new MutableLiveData<>();

    private final LiveData<NetworkResultWrapper<Boolean>> acceptResult;

    @Inject
    public VolunteerTaskViewModel() {
        this.acceptResult = Transformations.switchMap(missionTrigger, missionId -> {
            MutableLiveData<NetworkResultWrapper<Boolean>> mockResult = new MutableLiveData<>();
            mockResult.setValue(NetworkResultWrapper.success(true));
            return mockResult;
        });
    }

    public LiveData<Boolean> getIsMissionAccepted() {
        return isMissionAccepted;
    }

    public LiveData<Boolean> getIsMissionIgnored() {
        return isMissionIgnored;
    }

    public LiveData<String> getCurrentMissionType() {
        return currentMissionType;
    }

    public LiveData<Integer> getCurrentDeliveryStep() {
        return currentDeliveryStep;
    }

    public void setCurrentDeliveryStep(int step) {
        currentDeliveryStep.setValue(step);
    }

    public LiveData<NetworkResultWrapper<Boolean>> getAcceptResult() {
        return acceptResult;
    }

    public void acceptMission(String missionId, String missionType) {
        missionTrigger.setValue(missionId);
        currentMissionType.setValue(missionType);
        isMissionIgnored.setValue(false);
        isMissionAccepted.setValue(true);
    }

    public void declineMission() {
        currentMissionType.setValue(null);
        currentDeliveryStep.setValue(1);
        isMissionIgnored.setValue(true);
        isMissionAccepted.setValue(false);
    }

    public void completeMission() {
        currentMissionType.setValue(null);
        currentDeliveryStep.setValue(1);
        isMissionAccepted.setValue(false);
        isMissionIgnored.setValue(true);
    }
}
