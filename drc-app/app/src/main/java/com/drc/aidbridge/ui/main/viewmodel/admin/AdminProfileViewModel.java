package com.drc.aidbridge.ui.main.viewmodel.admin;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.domain.model.User;
import com.drc.aidbridge.domain.usecase.user.GetCachedUserUseCase;
import com.drc.aidbridge.ui.base.BaseViewModel;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class AdminProfileViewModel extends BaseViewModel {

    private final MutableLiveData<Long> loadProfileTrigger = new MutableLiveData<>();
    private final MediatorLiveData<String> email = new MediatorLiveData<>();
    private final MediatorLiveData<String> phone = new MediatorLiveData<>();

    private final LiveData<NetworkResultWrapper<User>> profileResult;

    @Inject
    public AdminProfileViewModel(GetCachedUserUseCase getCachedUserUseCase) {
        profileResult = Transformations.switchMap(
                loadProfileTrigger,
                ignored -> getCachedUserUseCase.execute());

        email.addSource(profileResult, result -> email.setValue(extractEmail(result)));
        phone.addSource(profileResult, result -> phone.setValue(extractPhone(result)));
    }

    public LiveData<String> getEmail() {
        return email;
    }

    public LiveData<String> getPhone() {
        return phone;
    }

    public void loadCurrentUserInfo() {
        loadProfileTrigger.setValue(System.currentTimeMillis());
    }

    private String extractEmail(NetworkResultWrapper<User> result) {
        if (result == null || !result.isSuccess() || result.getData() == null) {
            return "";
        }

        String userEmail = result.getData().getEmail();
        return userEmail != null ? userEmail.trim() : "";
    }

    private String extractPhone(NetworkResultWrapper<User> result) {
        if (result == null || !result.isSuccess() || result.getData() == null) {
            return "";
        }

        String userPhone = result.getData().getPhone();
        return userPhone != null ? userPhone.trim() : "";
    }
}