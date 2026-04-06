package com.drc.aidbridge.ui.main.viewmodel.volunteer;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.drc.aidbridge.data.mapper.volunteer.VolunteerDashboardInfoMapper;
import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.data.remote.dto.request.volunteer.ToggleStatusRequest;
import com.drc.aidbridge.data.remote.dto.response.volunteer.VolunteerProfileDataDto;
import com.drc.aidbridge.domain.model.volunteer.VolunteerDashboardInfo;
import com.drc.aidbridge.domain.model.volunteer.VolunteerPersonalInfo;
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
    private LiveData<NetworkResultWrapper<VolunteerProfileDataDto>> profileResultSource;
    private LiveData<NetworkResultWrapper<VolunteerDashboardInfo>> volunteerDashboardInfoResult;
    private LiveData<NetworkResultWrapper<VolunteerPersonalInfo>> volunteerPersonalInfoResult;
    private LiveData<NetworkResultWrapper<Boolean>> toggleStatusResult;

    private final VolunteerDashboardInfoMapper volunteerDashboardInfoMapper;

    @Inject
    public VolunteerDashboardViewModel(GetVolunteerDashboardInfoUseCase getVolunteerDashboardInfoUseCase,
            ToggleVolunteerStatusUseCase toggleVolunteerStatusUseCase,
            VolunteerDashboardInfoMapper volunteerDashboardInfoMapper) {
        this.volunteerDashboardInfoMapper = volunteerDashboardInfoMapper;
        initDashboardStreams(getVolunteerDashboardInfoUseCase);
        initToggleStreams(toggleVolunteerStatusUseCase);
    }

    private void initDashboardStreams(GetVolunteerDashboardInfoUseCase getVolunteerDashboardInfoUseCase) {
        profileResultSource = Transformations.switchMap(
                profileTrigger,
                trigger -> getVolunteerDashboardInfoUseCase.execute());

        volunteerDashboardInfoResult = Transformations.map(
                profileResultSource,
                this::mapDashboardResult);

        volunteerPersonalInfoResult = Transformations.map(
                profileResultSource,
                this::mapPersonalInfoResult);
    }

    private void initToggleStreams(ToggleVolunteerStatusUseCase toggleVolunteerStatusUseCase) {
        toggleStatusResult = Transformations.switchMap(
                toggleTrigger,
                toggleVolunteerStatusUseCase::execute);
    }

    public LiveData<NetworkResultWrapper<VolunteerDashboardInfo>> getVolunteerDashboardInfoResult() {
        return volunteerDashboardInfoResult;
    }

    public LiveData<NetworkResultWrapper<VolunteerPersonalInfo>> getVolunteerPersonalInfoResult() {
        return volunteerPersonalInfoResult;
    }

    public LiveData<NetworkResultWrapper<Boolean>> getToggleStatusResult() {
        return toggleStatusResult;
    }

    public void loadProfileDashboard() {
        profileTrigger.setValue(Boolean.TRUE);
    }

    public void toggleStatus(boolean isOnline) {
        toggleTrigger.setValue(new ToggleStatusRequest(isOnline, null, null));
    }

    private NetworkResultWrapper<VolunteerDashboardInfo> mapDashboardResult(
            NetworkResultWrapper<VolunteerProfileDataDto> result) {
        if (result == null) {
            return NetworkResultWrapper.error("Dữ liệu hồ sơ tình nguyện viên không hợp lệ.");
        }

        if (result.isLoading()) {
            return NetworkResultWrapper.loading();
        }

        if (result.isError()) {
            return NetworkResultWrapper.error(result.getMessage());
        }

        VolunteerProfileDataDto data = result.getData();
        if (data == null) {
            return NetworkResultWrapper.error("Dữ liệu hồ sơ tình nguyện viên trống.");
        }

        return NetworkResultWrapper.success(volunteerDashboardInfoMapper.mapToDomain(data));
    }

    private NetworkResultWrapper<VolunteerPersonalInfo> mapPersonalInfoResult(
            NetworkResultWrapper<VolunteerProfileDataDto> result) {
        if (result == null) {
            return NetworkResultWrapper.error("Dữ liệu hồ sơ cá nhân không hợp lệ.");
        }

        if (result.isLoading()) {
            return NetworkResultWrapper.loading();
        }

        if (result.isError()) {
            return NetworkResultWrapper.error(result.getMessage());
        }

        VolunteerProfileDataDto data = result.getData();
        if (data == null) {
            return NetworkResultWrapper.error("Dữ liệu hồ sơ cá nhân trống.");
        }

        return NetworkResultWrapper.success(volunteerDashboardInfoMapper.mapToPersonalInfo(data));
    }
}
