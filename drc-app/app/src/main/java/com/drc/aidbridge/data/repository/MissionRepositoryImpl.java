package com.drc.aidbridge.data.repository;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.drc.aidbridge.data.mapper.MissionMapper;
import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.data.remote.api.MissionApiService;
import com.drc.aidbridge.data.remote.dto.request.AcceptMissionRequest;
import com.drc.aidbridge.data.remote.dto.request.RejectMissionRequest;
import com.drc.aidbridge.data.remote.dto.response.BaseResponse;
import com.drc.aidbridge.data.remote.dto.response.MissionDto;
import com.drc.aidbridge.domain.model.VolunteerMission;
import com.drc.aidbridge.domain.repository.MissionRepository;

import javax.inject.Inject;
import javax.inject.Singleton;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@Singleton
public class MissionRepositoryImpl extends BaseRepository implements MissionRepository {

    private final MissionApiService missionApiService;
    private final MissionMapper missionMapper;

    @Inject
    public MissionRepositoryImpl(MissionApiService missionApiService,
                                 MissionMapper missionMapper) {
        this.missionApiService = missionApiService;
        this.missionMapper = missionMapper;
    }

    @Override
    public LiveData<NetworkResultWrapper<VolunteerMission>> getMission(String missionId) {
        MutableLiveData<NetworkResultWrapper<VolunteerMission>> result = new MutableLiveData<>();
        result.postValue(NetworkResultWrapper.loading());

        missionApiService.getMission(missionId).enqueue(new Callback<BaseResponse<MissionDto>>() {
            @Override
            public void onResponse(Call<BaseResponse<MissionDto>> call,
                                   Response<BaseResponse<MissionDto>> response) {
                handleMissionResponse(response, result, "Không thể tải nhiệm vụ.");
            }

            @Override
            public void onFailure(Call<BaseResponse<MissionDto>> call, Throwable t) {
                result.postValue(NetworkResultWrapper.error("Không thể tải nhiệm vụ: " + safeMessage(t)));
            }
        });

        return result;
    }

    @Override
    public LiveData<NetworkResultWrapper<VolunteerMission>> acceptMission(String missionId,
                                                                          String dispatchAttemptId,
                                                                          @Nullable Double currentLat,
                                                                          @Nullable Double currentLng) {
        MutableLiveData<NetworkResultWrapper<VolunteerMission>> result = new MutableLiveData<>();
        result.postValue(NetworkResultWrapper.loading());

        missionApiService.acceptMission(
                missionId,
                new AcceptMissionRequest(dispatchAttemptId, currentLat, currentLng)
        ).enqueue(new Callback<BaseResponse<MissionDto>>() {
            @Override
            public void onResponse(Call<BaseResponse<MissionDto>> call,
                                   Response<BaseResponse<MissionDto>> response) {
                handleMissionResponse(response, result, "Không thể chấp nhận nhiệm vụ.");
            }

            @Override
            public void onFailure(Call<BaseResponse<MissionDto>> call, Throwable t) {
                result.postValue(NetworkResultWrapper.error("Không thể chấp nhận nhiệm vụ: " + safeMessage(t)));
            }
        });

        return result;
    }

    @Override
    public LiveData<NetworkResultWrapper<Boolean>> rejectMission(String missionId,
                                                                 String dispatchAttemptId,
                                                                 String reason,
                                                                 @Nullable String reasonDetail) {
        MutableLiveData<NetworkResultWrapper<Boolean>> result = new MutableLiveData<>();
        result.postValue(NetworkResultWrapper.loading());

        missionApiService.rejectMission(
                missionId,
                new RejectMissionRequest(dispatchAttemptId, reason, reasonDetail)
        ).enqueue(new Callback<BaseResponse<Void>>() {
            @Override
            public void onResponse(Call<BaseResponse<Void>> call,
                                   Response<BaseResponse<Void>> response) {
                if (!response.isSuccessful()) {
                    result.postValue(NetworkResultWrapper.error(extractHttpError(response), response.code()));
                    return;
                }

                BaseResponse<Void> baseResponse = response.body();
                if (baseResponse == null) {
                    result.postValue(NetworkResultWrapper.error("Phản hồi từ chối nhiệm vụ không hợp lệ."));
                    return;
                }

                if (!baseResponse.isSuccess()) {
                    String apiMessage = baseResponse.getMessage();
                    result.postValue(NetworkResultWrapper.error(
                            apiMessage != null && !apiMessage.trim().isEmpty()
                                    ? apiMessage
                                    : "Từ chối nhiệm vụ thất bại."
                    ));
                    return;
                }

                result.postValue(NetworkResultWrapper.success(Boolean.TRUE));
            }

            @Override
            public void onFailure(Call<BaseResponse<Void>> call, Throwable t) {
                result.postValue(NetworkResultWrapper.error("Không thể từ chối nhiệm vụ: " + safeMessage(t)));
            }
        });

        return result;
    }

    private void handleMissionResponse(Response<BaseResponse<MissionDto>> response,
                                       MutableLiveData<NetworkResultWrapper<VolunteerMission>> result,
                                       String fallbackMessage) {
        if (!response.isSuccessful()) {
            result.postValue(NetworkResultWrapper.error(extractHttpError(response), response.code()));
            return;
        }

        BaseResponse<MissionDto> baseResponse = response.body();
        if (baseResponse == null) {
            result.postValue(NetworkResultWrapper.error(fallbackMessage));
            return;
        }

        if (!baseResponse.isSuccess()) {
            String apiMessage = baseResponse.getMessage();
            result.postValue(NetworkResultWrapper.error(
                    apiMessage != null && !apiMessage.trim().isEmpty()
                            ? apiMessage
                            : fallbackMessage
            ));
            return;
        }

        MissionDto missionDto = baseResponse.getData();
        if (missionDto == null) {
            result.postValue(NetworkResultWrapper.error(fallbackMessage));
            return;
        }

        result.postValue(NetworkResultWrapper.success(missionMapper.mapToDomain(missionDto)));
    }
}
