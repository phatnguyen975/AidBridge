package com.drc.aidbridge.ui.main.viewmodel.volunteer;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.domain.model.User;
import com.drc.aidbridge.domain.usecase.user.ChangePasswordUseCase;
import com.drc.aidbridge.domain.usecase.user.GetCachedUserUseCase;
import com.drc.aidbridge.domain.usecase.user.UpdateProfileUseCase;
import com.drc.aidbridge.domain.usecase.validation.AuthValidationResult;
import com.drc.aidbridge.ui.base.BaseViewModel;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class VolunteerPersonalInfoViewModel extends BaseViewModel {

    private final UpdateProfileUseCase updateProfileUseCase;
    private final ChangePasswordUseCase changePasswordUseCase;

    private final MutableLiveData<AuthValidationResult> validationError = new MutableLiveData<>();

    private final MutableLiveData<Long> loadUserTrigger = new MutableLiveData<>();
    private final MutableLiveData<UpdateProfileParams> updateProfileTrigger = new MutableLiveData<>();
    private final MutableLiveData<ChangePasswordParams> changePasswordTrigger = new MutableLiveData<>();

    private final LiveData<NetworkResultWrapper<User>> userInfoResult;
    private final LiveData<NetworkResultWrapper<User>> updateProfileResult;
    private final LiveData<NetworkResultWrapper<String>> changePasswordResult;

    @Inject
    public VolunteerPersonalInfoViewModel(GetCachedUserUseCase getCachedUserUseCase,
            UpdateProfileUseCase updateProfileUseCase,
            ChangePasswordUseCase changePasswordUseCase) {
        this.updateProfileUseCase = updateProfileUseCase;
        this.changePasswordUseCase = changePasswordUseCase;

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

    public LiveData<AuthValidationResult> getValidationError() {
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

    public void loadUserInfo() {
        loadUserTrigger.setValue(System.currentTimeMillis());
    }

    public void updateProfile(String name, String phone, String address) {
        AuthValidationResult validation = updateProfileUseCase.validate(name, phone);
        if (!validation.isValid()) {
            validationError.setValue(validation);
            return;
        }

        validationError.setValue(AuthValidationResult.valid());
        updateProfileTrigger.setValue(new UpdateProfileParams(name, phone, address));
    }

    public void changePassword(String currentPassword, String newPassword, String confirmPassword) {
        AuthValidationResult validation = changePasswordUseCase.validate(currentPassword, newPassword, confirmPassword);
        if (!validation.isValid()) {
            validationError.setValue(validation);
            return;
        }

        validationError.setValue(AuthValidationResult.valid());
        changePasswordTrigger.setValue(new ChangePasswordParams(currentPassword, newPassword));
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
