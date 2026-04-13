package com.drc.aidbridge.data.repository.volunteer;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.data.remote.api.volunteer.VolunteerApiService;
import com.drc.aidbridge.data.remote.dto.request.volunteer.ToggleStatusRequest;
import com.drc.aidbridge.data.remote.dto.response.volunteer.VolunteerHistoryItemDto;
import com.drc.aidbridge.data.remote.dto.response.volunteer.VolunteerHistoryResponseDto;
import com.drc.aidbridge.data.remote.dto.response.volunteer.ToggleStatusDataDto;
import com.drc.aidbridge.data.remote.dto.response.volunteer.ToggleStatusResponse;
import com.drc.aidbridge.data.remote.dto.response.volunteer.VolunteerProfileDataDto;
import com.drc.aidbridge.data.remote.dto.response.volunteer.VolunteerProfileResponse;
import com.drc.aidbridge.data.repository.BaseRepository;
import com.drc.aidbridge.domain.repository.volunteer.VolunteerRepository;

import javax.inject.Inject;
import javax.inject.Singleton;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.ArrayList;
import java.util.List;

@Singleton
public class VolunteerRepositoryImpl extends BaseRepository implements VolunteerRepository {

    private final VolunteerApiService volunteerApiService;

    @Inject
    public VolunteerRepositoryImpl(VolunteerApiService volunteerApiService) {
        this.volunteerApiService = volunteerApiService;
    }

    @Override
    public LiveData<NetworkResultWrapper<VolunteerProfileDataDto>> getVolunteerDashboardInfo() {
        MutableLiveData<NetworkResultWrapper<VolunteerProfileDataDto>> result = new MutableLiveData<>();
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

                result.postValue(NetworkResultWrapper.success(data));
            }

            @Override
            public void onFailure(Call<VolunteerProfileResponse> call, Throwable t) {
                result.postValue(NetworkResultWrapper.error("Không thể kết nối máy chủ: " + safeMessage(t)));
            }
        });

        return result;
    }

    @Override
    public LiveData<NetworkResultWrapper<List<VolunteerHistoryItemDto>>> getMissionHistory() {
        MutableLiveData<NetworkResultWrapper<List<VolunteerHistoryItemDto>>> result = new MutableLiveData<>();
        result.postValue(NetworkResultWrapper.loading());

        volunteerApiService.getMissionHistory().enqueue(new Callback<VolunteerHistoryResponseDto>() {
            @Override
            public void onResponse(Call<VolunteerHistoryResponseDto> call,
                    Response<VolunteerHistoryResponseDto> response) {
                if (!response.isSuccessful()) {
                    result.postValue(NetworkResultWrapper.error(extractHttpError(response), response.code()));
                    return;
                }

                VolunteerHistoryResponseDto body = response.body();
                if (body == null) {
                    result.postValue(NetworkResultWrapper.error("Phản hồi lịch sử nhiệm vụ không hợp lệ."));
                    return;
                }

                if (!body.isSuccess()) {
                    String message = body.getMessage();
                    result.postValue(NetworkResultWrapper.error(
                            message != null && !message.trim().isEmpty()
                                    ? message
                                    : "Không thể tải lịch sử nhiệm vụ."));
                    return;
                }

                List<VolunteerHistoryItemDto> data = body.getData();
                result.postValue(NetworkResultWrapper.success(data != null ? data : new ArrayList<>()));
            }

            @Override
            public void onFailure(Call<VolunteerHistoryResponseDto> call, Throwable t) {
                result.postValue(NetworkResultWrapper.error("Không thể kết nối máy chủ: " + safeMessage(t)));
            }
        });

        return result;
    }

    @Override
    public LiveData<NetworkResultWrapper<Boolean>> toggleStatus(ToggleStatusRequest request) {
        MutableLiveData<NetworkResultWrapper<Boolean>> result = new MutableLiveData<>();
        result.postValue(NetworkResultWrapper.loading());

        volunteerApiService.toggleVolunteerStatus(request).enqueue(new Callback<ToggleStatusResponse>() {
            @Override
            public void onResponse(Call<ToggleStatusResponse> call,
                    Response<ToggleStatusResponse> response) {
                if (!response.isSuccessful()) {
                    result.postValue(NetworkResultWrapper.error(extractHttpError(response), response.code()));
                    return;
                }

                ToggleStatusResponse baseResponse = response.body();
                if (baseResponse == null) {
                    result.postValue(NetworkResultWrapper.error("Phản hồi cập nhật trạng thái không hợp lệ."));
                    return;
                }

                if (!baseResponse.isSuccess()) {
                    String apiMessage = baseResponse.getMessage();
                    result.postValue(NetworkResultWrapper.error(
                            apiMessage != null && !apiMessage.trim().isEmpty()
                                    ? apiMessage
                                    : "Không thể cập nhật trạng thái hoạt động."));
                    return;
                }

                ToggleStatusDataDto data = baseResponse.getData();
                if (data == null) {
                    String apiMessage = baseResponse.getMessage();
                    result.postValue(NetworkResultWrapper.error(
                            apiMessage != null && !apiMessage.trim().isEmpty()
                                    ? apiMessage
                                    : "Dữ liệu trạng thái hoạt động trống."));
                    return;
                }

                ToggleStatusDataDto.ProfileDto profile = data.getProfile();
                if (profile == null) {
                    String apiMessage = baseResponse.getMessage();
                    result.postValue(NetworkResultWrapper.error(
                            apiMessage != null && !apiMessage.trim().isEmpty()
                                    ? apiMessage
                                    : "Dữ liệu hồ sơ trạng thái hoạt động trống."));
                    return;
                }

                result.postValue(NetworkResultWrapper.success(profile.isOnline()));
            }

            @Override
            public void onFailure(Call<ToggleStatusResponse> call, Throwable t) {
                result.postValue(NetworkResultWrapper.error("Không thể kết nối máy chủ: " + safeMessage(t)));
            }
        });

        return result;
    }
}
