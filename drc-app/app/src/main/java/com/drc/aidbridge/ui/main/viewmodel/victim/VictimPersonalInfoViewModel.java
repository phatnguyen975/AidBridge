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
import com.drc.aidbridge.domain.usecase.user.ChangePasswordUseCase;
import com.drc.aidbridge.domain.usecase.user.GetCachedUserUseCase;
import com.drc.aidbridge.domain.usecase.user.UpdateProfileUseCase;
import com.drc.aidbridge.domain.usecase.user.UploadAvatarUseCase;
import com.drc.aidbridge.domain.usecase.validation.ValidationResult;
import com.drc.aidbridge.ui.base.BaseViewModel;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class VictimPersonalInfoViewModel extends BaseViewModel {

    private final UpdateProfileUseCase updateProfileUseCase;
    private final ChangePasswordUseCase changePasswordUseCase;

    private final MutableLiveData<ValidationResult> validationError = new MutableLiveData<>();

    private final MutableLiveData<Long> loadUserTrigger = new MutableLiveData<>();
    private final MutableLiveData<UpdateProfileParams> updateProfileTrigger = new MutableLiveData<>();
    private final MutableLiveData<ChangePasswordParams> changePasswordTrigger = new MutableLiveData<>();

    private final LiveData<NetworkResultWrapper<User>> userInfoResult;
    private final LiveData<NetworkResultWrapper<User>> updateProfileResult;
    private final LiveData<NetworkResultWrapper<String>> changePasswordResult;
    private final MediatorLiveData<NetworkResultWrapper<String>> uploadAvatarResult = new MediatorLiveData<>();
    private final UploadAvatarUseCase uploadAvatarUseCase;
    private final ExecutorService uploadExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Inject
    public VictimPersonalInfoViewModel(GetCachedUserUseCase getCachedUserUseCase,
                                       UpdateProfileUseCase updateProfileUseCase,
                                       ChangePasswordUseCase changePasswordUseCase,
                                       UploadAvatarUseCase uploadAvatarUseCase) {
        this.updateProfileUseCase = updateProfileUseCase;
        this.changePasswordUseCase = changePasswordUseCase;
        this.uploadAvatarUseCase = uploadAvatarUseCase;

        this.userInfoResult = Transformations.switchMap(
            loadUserTrigger,
            ignored -> getCachedUserUseCase.execute()
        );

        this.updateProfileResult = Transformations.switchMap(
            updateProfileTrigger,
            params -> this.updateProfileUseCase.execute(params.name, params.phone, params.address)
        );

        this.changePasswordResult = Transformations.switchMap(
            changePasswordTrigger,
            params -> this.changePasswordUseCase.execute(params.currentPassword, params.newPassword)
        );
    }

    public LiveData<ValidationResult> getValidationError() {
        return validationError;
    }

    public LiveData<NetworkResultWrapper<User>> getUserInfoResult() {
        return userInfoResult;
    }

    public LiveData<NetworkResultWrapper<User>> getUpdateProfileResult() {
        return updateProfileResult;
    }

    public LiveData<NetworkResultWrapper<String>> getChangePasswordResult() {
        return changePasswordResult;
    }

    public LiveData<NetworkResultWrapper<String>> getUploadAvatarResult() {
        return uploadAvatarResult;
    }

    public void loadUserInfo() {
        loadUserTrigger.setValue(System.currentTimeMillis());
    }

    public void updateProfile(String name, String phone, String address) {
        ValidationResult validation = updateProfileUseCase.validate(name, phone);
        if (!validation.isValid()) {
            validationError.setValue(validation);
            return;
        }

        validationError.setValue(ValidationResult.valid());
        updateProfileTrigger.setValue(new UpdateProfileParams(name, phone, address));
    }

    public void changePassword(String currentPassword, String newPassword, String confirmPassword) {
        ValidationResult validation = changePasswordUseCase.validate(currentPassword, newPassword, confirmPassword);
        if (!validation.isValid()) {
            validationError.setValue(validation);
            return;
        }

        validationError.setValue(ValidationResult.valid());
        changePasswordTrigger.setValue(new ChangePasswordParams(currentPassword, newPassword));
    }

    public void uploadAvatar(Context context, Uri imageUri) {
        validationError.postValue(ValidationResult.valid());
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

    private static final class UpdateProfileParams {
        final String name;
        final String phone;
        final String address;

        UpdateProfileParams(String name, String phone, String address) {
            this.name = name;
            this.phone = phone;
            this.address = address;
        }
    }

    private static final class ChangePasswordParams {
        final String currentPassword;
        final String newPassword;

        ChangePasswordParams(String currentPassword, String newPassword) {
            this.currentPassword = currentPassword;
            this.newPassword = newPassword;
        }
    }
}
