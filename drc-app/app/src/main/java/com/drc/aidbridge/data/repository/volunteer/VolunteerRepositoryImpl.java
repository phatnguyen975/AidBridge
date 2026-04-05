package com.drc.aidbridge.data.repository.volunteer;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.drc.aidbridge.data.mapper.volunteer.VolunteerDashboardInfoMapper;
import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.data.remote.api.volunteer.VolunteerApiService;
import com.drc.aidbridge.data.remote.dto.response.volunteer.VolunteerProfileDataDto;
import com.drc.aidbridge.data.remote.dto.response.volunteer.VolunteerProfileResponse;
import com.drc.aidbridge.data.repository.BaseRepository;
import com.drc.aidbridge.domain.model.volunteer.VolunteerDashboardInfo;
import com.drc.aidbridge.domain.repository.volunteer.VolunteerRepository;

import javax.inject.Inject;
import javax.inject.Singleton;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@Singleton
public class VolunteerRepositoryImpl extends BaseRepository implements VolunteerRepository {

    private final VolunteerApiService volunteerApiService;
    private final VolunteerDashboardInfoMapper volunteerDashboardInfoMapper;

    @Inject
    public VolunteerRepositoryImpl(VolunteerApiService volunteerApiService,
            VolunteerDashboardInfoMapper volunteerDashboardInfoMapper) {
        this.volunteerApiService = volunteerApiService;
        this.volunteerDashboardInfoMapper = volunteerDashboardInfoMapper;
    }

    @Override
    public LiveData<NetworkResultWrapper<VolunteerDashboardInfo>> getVolunteerDashboardInfo() {
        MutableLiveData<NetworkResultWrapper<VolunteerDashboardInfo>> result = new MutableLiveData<>();
        result.postValue(NetworkResultWrapper.loading());

        volunteerApiService.getVolunteerProfile().enqueue(new Callback<VolunteerProfileResponse>() {
            @Override
            public void onResponse(Call<VolunteerProfileResponse> call,
                    Response<VolunteerProfileResponse> response) {
                if (!response.isSuccessful()) {
                    result.postValue(NetworkResultWrapper.error(extractHttpError(response), response.code()));
                    return;
                }

                VolunteerProfileResponse baseResponse = response.body();
                if (baseResponse == null) {
                    result.postValue(NetworkResultWrapper.error("Phản hồi hồ sơ tình nguyện viên không hợp lệ."));
                    return;
                }

                if (!baseResponse.isSuccess()) {
                    String apiMessage = baseResponse.getMessage();
                    result.postValue(NetworkResultWrapper.error(
                            apiMessage != null && !apiMessage.trim().isEmpty()
                                    ? apiMessage
                                    : "Không thể tải hồ sơ tình nguyện viên."));
                    return;
                }

                VolunteerProfileDataDto data = baseResponse.getData();
                if (data == null) {
                    String apiMessage = baseResponse.getMessage();
                    result.postValue(NetworkResultWrapper.error(
                            apiMessage != null && !apiMessage.trim().isEmpty()
                                    ? apiMessage
                                    : "Dữ liệu hồ sơ tình nguyện viên trống."));
                    return;
                }

                VolunteerDashboardInfo dashboardInfo = volunteerDashboardInfoMapper.mapToDomain(data);
                result.postValue(NetworkResultWrapper.success(dashboardInfo));
            }

            @Override
            public void onFailure(Call<VolunteerProfileResponse> call, Throwable t) {
                result.postValue(NetworkResultWrapper.error("Không thể kết nối máy chủ: " + safeMessage(t)));
            }
        });

        return result;
    }
}
