package com.drc.aidbridge.ui.main.viewmodel.sponsor;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.domain.model.User;
import com.drc.aidbridge.domain.usecase.user.GetCurrentUserUseCase;
import com.drc.aidbridge.ui.base.BaseViewModel;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class SponsorProfileViewModel extends BaseViewModel {

    private final MutableLiveData<Long> loadProfileTrigger = new MutableLiveData<>();
    private final LiveData<NetworkResultWrapper<User>> userLiveData;

    @Inject
    public SponsorProfileViewModel(GetCurrentUserUseCase getCurrentUserUseCase) {
        userLiveData = Transformations.switchMap(
                loadProfileTrigger,
                ignored -> getCurrentUserUseCase.execute());
    }

    public LiveData<NetworkResultWrapper<User>> getUserLiveData() {
        return userLiveData;
    }

    public void loadProfile() {
        loadProfileTrigger.setValue(System.currentTimeMillis());
    }
}
