package com.drc.aidbridge.data.repository.volunteer;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.data.remote.api.volunteer.VolunteerApiService;
import com.drc.aidbridge.data.remote.dto.request.volunteer.AcceptDispatchAttemptRequest;
import com.drc.aidbridge.data.remote.dto.request.volunteer.CancelDispatchAttemptRequest;
import com.drc.aidbridge.data.remote.dto.request.volunteer.ToggleStatusRequest;
import com.drc.aidbridge.data.remote.dto.response.volunteer.LatestDispatchDataDto;
import com.drc.aidbridge.data.remote.dto.response.volunteer.LatestDispatchResponse;
import com.drc.aidbridge.data.remote.dto.response.volunteer.VolunteerHistoryDataDto;
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
    public LiveData<NetworkResultWrapper<VolunteerHistoryDataDto>> getMissionHistory(int page, int limit) {
        MutableLiveData<NetworkResultWrapper<VolunteerHistoryDataDto>> result = new MutableLiveData<>();
        result.postValue(NetworkResultWrapper.loading());

        volunteerApiService.getMissionHistory(page, limit).enqueue(new Callback<VolunteerHistoryResponseDto>() {
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

                VolunteerHistoryDataDto data = body.getData();
                if (data == null) {
                    String message = body.getMessage();
                    result.postValue(NetworkResultWrapper.error(
                            message != null && !message.trim().isEmpty()
                                    ? message
                                    : "Dữ liệu lịch sử nhiệm vụ trống."));
                    return;
                }

                result.postValue(NetworkResultWrapper.success(data));
            }

            @Override
            public void onFailure(Call<VolunteerHistoryResponseDto> call, Throwable t) {
                result.postValue(NetworkResultWrapper.error("Không thể kết nối máy chủ: " + safeMessage(t)));
            }
        });

        return result;
    }

    @Override
    public LiveData<NetworkResultWrapper<com.drc.aidbridge.data.remote.dto.response.volunteer.MissionHistoryFullDataDto>> getMissionHistoryFull(int page, int limit) {
        MutableLiveData<NetworkResultWrapper<com.drc.aidbridge.data.remote.dto.response.volunteer.MissionHistoryFullDataDto>> result = new MutableLiveData<>();
        result.postValue(NetworkResultWrapper.loading());

        volunteerApiService.getMissionHistoryFull(page, limit).enqueue(new Callback<com.drc.aidbridge.data.remote.dto.response.volunteer.MissionHistoryFullResponseDto>() {
            @Override
            public void onResponse(Call<com.drc.aidbridge.data.remote.dto.response.volunteer.MissionHistoryFullResponseDto> call,
                    Response<com.drc.aidbridge.data.remote.dto.response.volunteer.MissionHistoryFullResponseDto> response) {
                if (!response.isSuccessful()) {
                    result.postValue(NetworkResultWrapper.error(extractHttpError(response), response.code()));
                    return;
                }

                com.drc.aidbridge.data.remote.dto.response.volunteer.MissionHistoryFullResponseDto body = response.body();
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

                com.drc.aidbridge.data.remote.dto.response.volunteer.MissionHistoryFullDataDto data = body.getData();
                if (data == null) {
                    String message = body.getMessage();
                    result.postValue(NetworkResultWrapper.error(
                            message != null && !message.trim().isEmpty()
                                    ? message
                                    : "Dữ liệu lịch sử nhiệm vụ trống."));
                    return;
                }

                result.postValue(NetworkResultWrapper.success(data));
            }

            @Override
            public void onFailure(Call<com.drc.aidbridge.data.remote.dto.response.volunteer.MissionHistoryFullResponseDto> call, Throwable t) {
                result.postValue(NetworkResultWrapper.error("Không thể kết nối máy chủ: " + safeMessage(t)));
            }
        });

        return result;
    }

    @Override
    public LiveData<NetworkResultWrapper<com.drc.aidbridge.data.remote.dto.response.volunteer.MissionHistoryFullItemDto>> getCurrentMission() {
        MutableLiveData<NetworkResultWrapper<com.drc.aidbridge.data.remote.dto.response.volunteer.MissionHistoryFullItemDto>> result = new MutableLiveData<>();
        result.postValue(NetworkResultWrapper.loading());

        volunteerApiService.getCurrentMission().enqueue(new Callback<com.drc.aidbridge.data.remote.dto.response.volunteer.CurrentMissionResponseDto>() {
            @Override
            public void onResponse(Call<com.drc.aidbridge.data.remote.dto.response.volunteer.CurrentMissionResponseDto> call,
                    Response<com.drc.aidbridge.data.remote.dto.response.volunteer.CurrentMissionResponseDto> response) {
                if (!response.isSuccessful()) {
                    result.postValue(NetworkResultWrapper.error(extractHttpError(response), response.code()));
                    return;
                }

                com.drc.aidbridge.data.remote.dto.response.volunteer.CurrentMissionResponseDto body = response.body();
                if (body == null) {
                    result.postValue(NetworkResultWrapper.error("Phản hồi nhiệm vụ hiện tại không hợp lệ."));
                    return;
                }

                if (!body.isSuccess()) {
                    String message = body.getMessage();
                    result.postValue(NetworkResultWrapper.error(
                            message != null && !message.trim().isEmpty()
                                    ? message
                                    : "Không thể tải nhiệm vụ hiện tại."));
                    return;
                }

                com.drc.aidbridge.data.remote.dto.response.volunteer.MissionHistoryFullItemDto data = body.getData();
                if (data == null) {
                    String message = body.getMessage();
                    result.postValue(NetworkResultWrapper.error(
                            message != null && !message.trim().isEmpty()
                                    ? message
                                    : "Không có nhiệm vụ hiện tại nào đang diễn ra."));
                    return;
                }

                result.postValue(NetworkResultWrapper.success(data));
            }

            @Override
            public void onFailure(Call<com.drc.aidbridge.data.remote.dto.response.volunteer.CurrentMissionResponseDto> call, Throwable t) {
                result.postValue(NetworkResultWrapper.error("Không thể kết nối máy chủ: " + safeMessage(t)));
            }
        });

        return result;
    }

    @Override
    public LiveData<NetworkResultWrapper<com.drc.aidbridge.data.remote.dto.response.MissionDto>> completeMission(String missionId, String notes) {
        MutableLiveData<NetworkResultWrapper<com.drc.aidbridge.data.remote.dto.response.MissionDto>> result = new MutableLiveData<>();
        result.postValue(NetworkResultWrapper.loading());

        com.drc.aidbridge.data.remote.dto.request.volunteer.CompleteMissionRequestDto request = 
            new com.drc.aidbridge.data.remote.dto.request.volunteer.CompleteMissionRequestDto(missionId, notes);

        volunteerApiService.completeMission(request).enqueue(new Callback<com.drc.aidbridge.data.remote.dto.response.volunteer.CompleteMissionResponseDto>() {
            @Override
            public void onResponse(Call<com.drc.aidbridge.data.remote.dto.response.volunteer.CompleteMissionResponseDto> call,
                    Response<com.drc.aidbridge.data.remote.dto.response.volunteer.CompleteMissionResponseDto> response) {
                if (!response.isSuccessful()) {
                    result.postValue(NetworkResultWrapper.error(extractHttpError(response), response.code()));
                    return;
                }

                com.drc.aidbridge.data.remote.dto.response.volunteer.CompleteMissionResponseDto body = response.body();
                if (body == null) {
                    result.postValue(NetworkResultWrapper.error("Phản hồi hoàn thành nhiệm vụ không hợp lệ."));
                    return;
                }

                if (!body.isSuccess()) {
                    String message = body.getMessage();
                    result.postValue(NetworkResultWrapper.error(
                            message != null && !message.trim().isEmpty() ? message : "Không thể hoàn thành nhiệm vụ."));
                    return;
                }

                result.postValue(NetworkResultWrapper.success(body.getData()));
            }

            @Override
            public void onFailure(Call<com.drc.aidbridge.data.remote.dto.response.volunteer.CompleteMissionResponseDto> call, Throwable t) {
                result.postValue(NetworkResultWrapper.error("Không thể kết nối máy chủ: " + safeMessage(t)));
            }
        });

        return result;
    }

    @Override
    public LiveData<NetworkResultWrapper<com.drc.aidbridge.data.remote.dto.response.MissionDto>> cancelMission(String missionId, String reason) {
        MutableLiveData<NetworkResultWrapper<com.drc.aidbridge.data.remote.dto.response.MissionDto>> result = new MutableLiveData<>();
        result.postValue(NetworkResultWrapper.loading());

        com.drc.aidbridge.data.remote.dto.request.volunteer.CancelMissionRequestDto request = 
            new com.drc.aidbridge.data.remote.dto.request.volunteer.CancelMissionRequestDto(missionId, reason);

        volunteerApiService.cancelMission(request).enqueue(new Callback<com.drc.aidbridge.data.remote.dto.response.volunteer.CancelMissionResponseDto>() {
            @Override
            public void onResponse(Call<com.drc.aidbridge.data.remote.dto.response.volunteer.CancelMissionResponseDto> call,
                    Response<com.drc.aidbridge.data.remote.dto.response.volunteer.CancelMissionResponseDto> response) {
                if (!response.isSuccessful()) {
                    result.postValue(NetworkResultWrapper.error(extractHttpError(response), response.code()));
                    return;
                }

                com.drc.aidbridge.data.remote.dto.response.volunteer.CancelMissionResponseDto body = response.body();
                if (body == null) {
                    result.postValue(NetworkResultWrapper.error("Phản hồi hủy nhiệm vụ không hợp lệ."));
                    return;
                }

                if (!body.isSuccess()) {
                    String message = body.getMessage();
                    result.postValue(NetworkResultWrapper.error(
                            message != null && !message.trim().isEmpty() ? message : "Không thể hủy nhiệm vụ."));
                    return;
                }

                result.postValue(NetworkResultWrapper.success(body.getData()));
            }

            @Override
            public void onFailure(Call<com.drc.aidbridge.data.remote.dto.response.volunteer.CancelMissionResponseDto> call, Throwable t) {
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

    @Override
    public LiveData<NetworkResultWrapper<LatestDispatchDataDto>> getLatestDispatch() {
        MutableLiveData<NetworkResultWrapper<LatestDispatchDataDto>> result = new MutableLiveData<>();
        result.postValue(NetworkResultWrapper.loading());

        volunteerApiService.getLatestDispatch().enqueue(new Callback<LatestDispatchResponse>() {
            @Override
            public void onResponse(Call<LatestDispatchResponse> call,
                                   Response<LatestDispatchResponse> response) {
                if (!response.isSuccessful()) {
                    result.postValue(NetworkResultWrapper.error(extractHttpError(response), response.code()));
                    return;
                }

                LatestDispatchResponse baseResponse = response.body();
                if (baseResponse == null) {
                    result.postValue(NetworkResultWrapper.error("Phản hồi không hợp lệ."));
                    return;
                }

                if (!baseResponse.isSuccess()) {
                    result.postValue(NetworkResultWrapper.error(baseResponse.getMessage()));
                    return;
                }

                result.postValue(NetworkResultWrapper.success(baseResponse.getData()));
            }

            @Override
            public void onFailure(Call<LatestDispatchResponse> call, Throwable t) {
                result.postValue(NetworkResultWrapper.error("Không thể kết nối máy chủ: " + safeMessage(t)));
            }
        });

        return result;
    }

    @Override
    public LiveData<NetworkResultWrapper<LatestDispatchDataDto>> cancelDispatchAttempt(String dispatchAttemptId) {
        MutableLiveData<NetworkResultWrapper<LatestDispatchDataDto>> result = new MutableLiveData<>();
        result.postValue(NetworkResultWrapper.loading());

        CancelDispatchAttemptRequest request = new CancelDispatchAttemptRequest(dispatchAttemptId);

        volunteerApiService.cancelDispatchAttempt(request).enqueue(new Callback<LatestDispatchResponse>() {
            @Override
            public void onResponse(Call<LatestDispatchResponse> call,
                                   Response<LatestDispatchResponse> response) {
                if (!response.isSuccessful()) {
                    result.postValue(NetworkResultWrapper.error(extractHttpError(response), response.code()));
                    return;
                }

                LatestDispatchResponse baseResponse = response.body();
                if (baseResponse == null) {
                    result.postValue(NetworkResultWrapper.error("Phản hồi không hợp lệ."));
                    return;
                }

                if (!baseResponse.isSuccess()) {
                    result.postValue(NetworkResultWrapper.error(baseResponse.getMessage()));
                    return;
                }

                result.postValue(NetworkResultWrapper.success(baseResponse.getData()));
            }

            @Override
            public void onFailure(Call<LatestDispatchResponse> call, Throwable t) {
                result.postValue(NetworkResultWrapper.error("Không thể kết nối máy chủ: " + safeMessage(t)));
            }
        });

        return result;
    }

    @Override
    public LiveData<NetworkResultWrapper<LatestDispatchDataDto>> acceptDispatchAttempt(String dispatchAttemptId) {
        MutableLiveData<NetworkResultWrapper<LatestDispatchDataDto>> result = new MutableLiveData<>();
        result.postValue(NetworkResultWrapper.loading());

        AcceptDispatchAttemptRequest request = new AcceptDispatchAttemptRequest(dispatchAttemptId);

        volunteerApiService.acceptDispatchAttempt(request).enqueue(new Callback<LatestDispatchResponse>() {
            @Override
            public void onResponse(Call<LatestDispatchResponse> call,
                                   Response<LatestDispatchResponse> response) {
                if (!response.isSuccessful()) {
                    result.postValue(NetworkResultWrapper.error(extractHttpError(response), response.code()));
                    return;
                }

                LatestDispatchResponse baseResponse = response.body();
                if (baseResponse == null) {
                    result.postValue(NetworkResultWrapper.error("Phản hồi không hợp lệ."));
                    return;
                }

                if (!baseResponse.isSuccess()) {
                    result.postValue(NetworkResultWrapper.error(baseResponse.getMessage()));
                    return;
                }

                result.postValue(NetworkResultWrapper.success(baseResponse.getData()));
            }

            @Override
            public void onFailure(Call<LatestDispatchResponse> call, Throwable t) {
                result.postValue(NetworkResultWrapper.error("Không thể kết nối máy chủ: " + safeMessage(t)));
            }
        });

        return result;
    }
}
