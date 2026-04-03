package com.drc.aidbridge.ui.main.viewmodel.victim;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.domain.model.User;
import com.drc.aidbridge.domain.usecase.user.GetCachedUserUseCase;
import com.drc.aidbridge.domain.usecase.user.UploadAvatarUseCase;
import com.drc.aidbridge.ui.base.BaseViewModel;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class VictimProfileViewModel extends BaseViewModel {

    private final MutableLiveData<Long> loadUserTrigger = new MutableLiveData<>();
    private final LiveData<NetworkResultWrapper<User>> userInfoResult;
    private final MediatorLiveData<NetworkResultWrapper<String>> uploadAvatarResult = new MediatorLiveData<>();
    private final UploadAvatarUseCase uploadAvatarUseCase;
    private final ExecutorService uploadExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Inject
    public VictimProfileViewModel(GetCachedUserUseCase getCachedUserUseCase,
                                  UploadAvatarUseCase uploadAvatarUseCase) {
        this.uploadAvatarUseCase = uploadAvatarUseCase;

        userInfoResult = Transformations.switchMap(
            loadUserTrigger,
            ignored -> getCachedUserUseCase.execute()
        );
    }

    public LiveData<NetworkResultWrapper<User>> getUserInfoResult() {
        return userInfoResult;
    }

    public LiveData<NetworkResultWrapper<String>> getUploadAvatarResult() {
        return uploadAvatarResult;
    }

    public void loadUserInfo() {
        loadUserTrigger.setValue(System.currentTimeMillis());
    }

    public void uploadAvatar(Context context, Uri imageUri) {
        AvatarUploadCoordinator.uploadAvatar(
            context,
            imageUri,
            uploadExecutor,
            mainHandler,
            uploadAvatarResult,
            uploadAvatarUseCase
        );
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        uploadExecutor.shutdownNow();
    }
}
