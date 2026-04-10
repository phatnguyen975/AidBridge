package com.drc.aidbridge.domain.usecase.volunteer;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;

import com.drc.aidbridge.data.mapper.volunteer.VolunteerInfoMapper;
import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.domain.model.volunteer.VolunteerDashboardInfo;
import com.drc.aidbridge.domain.repository.volunteer.VolunteerRepository;

import javax.inject.Inject;

public class GetVolunteerDashboardInfoUseCase {

    private final VolunteerRepository volunteerRepository;
    private final VolunteerInfoMapper volunteerInfoMapper;

    @Inject
    public GetVolunteerDashboardInfoUseCase(VolunteerRepository volunteerRepository,
            VolunteerInfoMapper volunteerInfoMapper) {
        this.volunteerRepository = volunteerRepository;
        this.volunteerInfoMapper = volunteerInfoMapper;
    }

    public LiveData<NetworkResultWrapper<VolunteerDashboardInfo>> execute() {
        return Transformations.map(
                volunteerRepository.getVolunteerDashboardInfo(),
                result -> {
                    if (result == null) {
                        return NetworkResultWrapper.error("Dữ liệu hồ sơ tình nguyện viên không hợp lệ.");
                    }

                    if (result.isLoading()) {
                        return NetworkResultWrapper.loading();
                    }

                    if (result.isError()) {
                        return NetworkResultWrapper.error(result.getMessage());
                    }

                    return NetworkResultWrapper.success(
                            volunteerInfoMapper.mapToDashboardInfoDomain(result.getData()));
                });
    }
}
