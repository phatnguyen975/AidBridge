package com.drc.aidbridge.ui.main.viewmodel.staff;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.data.remote.api.staff.StaffApiService;
import com.drc.aidbridge.data.remote.dto.response.BaseResponse;
import com.drc.aidbridge.data.remote.dto.response.staff.StaffProfileDto;
import com.drc.aidbridge.domain.model.User;
import com.drc.aidbridge.domain.usecase.user.GetCurrentUserUseCase;
import com.drc.aidbridge.ui.base.BaseViewModel;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@HiltViewModel
public class StaffProfileViewModel extends BaseViewModel {

    private final MutableLiveData<Long> loadProfileTrigger = new MutableLiveData<>();
    private final MutableLiveData<NetworkResultWrapper<String>> staffStartDateLiveData = new MutableLiveData<>();
    private final LiveData<NetworkResultWrapper<User>> userLiveData;
    private final StaffApiService staffApiService;
    private Call<BaseResponse<StaffProfileDto>> staffProfileCall;

    @Inject
    public StaffProfileViewModel(GetCurrentUserUseCase getCurrentUserUseCase,
                                 StaffApiService staffApiService) {
        this.staffApiService = staffApiService;
        userLiveData = Transformations.switchMap(
                loadProfileTrigger,
                ignored -> getCurrentUserUseCase.execute());
    }

    public LiveData<NetworkResultWrapper<User>> getUserLiveData() {
        return userLiveData;
    }

    public LiveData<NetworkResultWrapper<String>> getStaffStartDateLiveData() {
        return staffStartDateLiveData;
    }

    public void loadProfile() {
        loadProfileTrigger.setValue(System.currentTimeMillis());
    }

    public void loadStaffStartDate(String userId) {
        String safeUserId = userId != null ? userId.trim() : "";
        if (safeUserId.isEmpty()) {
            return;
        }

        if (staffProfileCall != null) {
            staffProfileCall.cancel();
        }

        staffStartDateLiveData.setValue(NetworkResultWrapper.loading());
        staffProfileCall = staffApiService.getStaffByUser(safeUserId);
        staffProfileCall.enqueue(new Callback<BaseResponse<StaffProfileDto>>() {
            @Override
            public void onResponse(Call<BaseResponse<StaffProfileDto>> call,
                                   Response<BaseResponse<StaffProfileDto>> response) {
                if (call.isCanceled()) {
                    return;
                }

                if (!response.isSuccessful()) {
                    staffStartDateLiveData.postValue(
                            NetworkResultWrapper.error("Khong the tai ngay bat dau tham gia.", response.code()));
                    return;
                }

                BaseResponse<StaffProfileDto> body = response.body();
                StaffProfileDto data = body != null ? body.getData() : null;
                String startDate = data != null ? data.getStartDate() : null;
                if (body == null || !body.isSuccess() || startDate == null || startDate.trim().isEmpty()) {
                    staffStartDateLiveData.postValue(
                            NetworkResultWrapper.error("Khong nhan duoc ngay bat dau tham gia."));
                    return;
                }

                staffStartDateLiveData.postValue(NetworkResultWrapper.success(startDate.trim()));
            }

            @Override
            public void onFailure(Call<BaseResponse<StaffProfileDto>> call, Throwable throwable) {
                if (call.isCanceled()) {
                    return;
                }

                staffStartDateLiveData.postValue(
                        NetworkResultWrapper.error("Khong the tai ngay bat dau tham gia."));
            }
        });
    }

    @Override
    protected void onCleared() {
        if (staffProfileCall != null) {
            staffProfileCall.cancel();
        }
        super.onCleared();
    }
}
