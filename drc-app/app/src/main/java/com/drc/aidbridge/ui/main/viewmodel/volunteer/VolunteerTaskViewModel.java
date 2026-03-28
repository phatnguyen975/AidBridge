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

    public LiveData<NetworkResultWrapper<Boolean>> getAcceptResult() {
        return acceptResult;
    }

    public void acceptMission(String missionId) {
        missionTrigger.setValue(missionId);
        isMissionIgnored.setValue(false);
        isMissionAccepted.setValue(true);
    }

    public void declineMission() {
        isMissionIgnored.setValue(true);
        isMissionAccepted.setValue(false);
    }

    public void completeMission() {
        isMissionAccepted.setValue(false);
    }
}
