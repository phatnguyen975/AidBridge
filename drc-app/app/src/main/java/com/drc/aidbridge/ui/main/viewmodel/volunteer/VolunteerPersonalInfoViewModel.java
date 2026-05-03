package com.drc.aidbridge.ui.main.viewmodel.volunteer;

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
import com.drc.aidbridge.utils.ImageUtils;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import okhttp3.MultipartBody;

@HiltViewModel
public class VolunteerPersonalInfoViewModel extends BaseViewModel {

    private final UpdateProfileUseCase updateProfileUseCase;
    private final ChangePasswordUseCase changePasswordUseCase;

    private final MutableLiveData<ValidationResult> validationError = new MutableLiveData<>();

    private final MutableLiveData<Long> loadUserTrigger = new MutableLiveData<>();
    private final MutableLiveData<UpdateProfileParams> updateProfileTrigger = new MutableLiveData<>();
    private final MutableLiveData<ChangePasswordParams> changePasswordTrigger = new MutableLiveData<>();

    private final LiveData<NetworkResultWrapper<User>> userInfoResult;
    private final LiveData<NetworkResultWrapper<User>> updateProfileResult;
    private final LiveData<NetworkResultWrapper<String>> changePasswordResult;
    public final MediatorLiveData<NetworkResultWrapper<String>> uploadAvatarResult = new MediatorLiveData<>();
    private final UploadAvatarUseCase uploadAvatarUseCase;
    private final ExecutorService uploadExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Inject
    public VolunteerPersonalInfoViewModel(GetCachedUserUseCase getCachedUserUseCase,
            UpdateProfileUseCase updateProfileUseCase,
            ChangePasswordUseCase changePasswordUseCase,
            UploadAvatarUseCase uploadAvatarUseCase) {
        this.updateProfileUseCase = updateProfileUseCase;
        this.changePasswordUseCase = changePasswordUseCase;
        this.uploadAvatarUseCase = uploadAvatarUseCase;

        this.userInfoResult = Transformations.switchMap(
                loadUserTrigger,
                ignored -> getCachedUserUseCase.execute());

        this.updateProfileResult = Transformations.switchMap(
                updateProfileTrigger,
                params -> this.updateProfileUseCase.execute(params.name, params.phone, params.address));

        this.changePasswordResult = Transformations.switchMap(
                changePasswordTrigger,
                params -> this.changePasswordUseCase.execute(params.currentPassword, params.newPassword));
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
        uploadAvatarResult.postValue(NetworkResultWrapper.loading());

        uploadExecutor.execute(() -> {
            File compressedFile = null;
            try {
                compressedFile = ImageUtils.compressAvatar(context.getApplicationContext(), imageUri);
                MultipartBody.Part avatarPart = ImageUtils.createAvatarMultipart(compressedFile);
                LiveData<NetworkResultWrapper<String>> source = uploadAvatarUseCase.execute(avatarPart);
                File finalCompressedFile = compressedFile;

                mainHandler.post(() -> uploadAvatarResult.addSource(source, result -> {
                    uploadAvatarResult.postValue(result);
                    if (result != null && !result.isLoading()) {
                        uploadAvatarResult.removeSource(source);
                        if (finalCompressedFile.exists()) {
                            // Best-effort cleanup for temporary compressed files.
                            finalCompressedFile.delete();
                        }
                    }
                }));
            } catch (IOException exception) {
                cleanup(compressedFile);
                uploadAvatarResult.postValue(NetworkResultWrapper.error(
                        "Khong the nen anh dai dien: " + safeMessage(exception)));
            } catch (Exception exception) {
                cleanup(compressedFile);
                uploadAvatarResult.postValue(NetworkResultWrapper.error(
                        "Tai anh dai dien that bai: " + safeMessage(exception)));
            }
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        uploadExecutor.shutdownNow();
    }

    private void cleanup(File file) {
        if (file != null && file.exists()) {
            file.delete();
        }
    }

    private String safeMessage(Throwable throwable) {
        String message = throwable.getMessage();
        if (message == null || message.trim().isEmpty()) {
            return "Loi khong xac dinh";
        }
        return message.trim();
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
