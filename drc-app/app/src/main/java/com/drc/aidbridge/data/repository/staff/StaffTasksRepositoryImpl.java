package com.drc.aidbridge.data.repository.staff;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.data.remote.api.staff.StaffApiService;
import com.drc.aidbridge.data.remote.dto.response.BaseResponse;
import com.drc.aidbridge.data.remote.dto.response.staff.StaffUpcomingDeliveryMissionDto;
import com.drc.aidbridge.data.remote.dto.response.staff.StaffUpcomingDonationDto;
import com.drc.aidbridge.data.repository.BaseRepository;
import com.drc.aidbridge.domain.model.staff.StaffUpcomingTask;
import com.drc.aidbridge.domain.repository.staff.StaffTasksRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@Singleton
public class StaffTasksRepositoryImpl extends BaseRepository implements StaffTasksRepository {

    private final StaffApiService apiService;

    @Inject
    public StaffTasksRepositoryImpl(StaffApiService apiService) {
        this.apiService = apiService;
    }

    @Override
    public LiveData<NetworkResultWrapper<List<StaffUpcomingTask>>> getUpcomingDonations(int page, int limit) {
        MutableLiveData<NetworkResultWrapper<List<StaffUpcomingTask>>> result = new MutableLiveData<>();
        result.postValue(NetworkResultWrapper.loading());

        apiService.getUpcomingDonations(normalizePage(page), normalizeLimit(limit))
                .enqueue(new Callback<BaseResponse<List<StaffUpcomingDonationDto>>>() {
                    @Override
                    public void onResponse(Call<BaseResponse<List<StaffUpcomingDonationDto>>> call,
                                           Response<BaseResponse<List<StaffUpcomingDonationDto>>> response) {
                        if (!response.isSuccessful()) {
                            result.postValue(NetworkResultWrapper.error(
                                    extractHttpError(response),
                                    response.code()
                            ));
                            return;
                        }

                        BaseResponse<List<StaffUpcomingDonationDto>> body = response.body();
                        if (body == null || !body.isSuccess()) {
                            result.postValue(NetworkResultWrapper.error(
                                    body != null ? body.getMessage() : extractHttpError(response)
                            ));
                            return;
                        }

                        result.postValue(NetworkResultWrapper.success(mapDonations(body.getData())));
                    }

                    @Override
                    public void onFailure(Call<BaseResponse<List<StaffUpcomingDonationDto>>> call, Throwable t) {
                        result.postValue(NetworkResultWrapper.error(safeMessage(t)));
                    }
                });

        return result;
    }

    @Override
    public LiveData<NetworkResultWrapper<List<StaffUpcomingTask>>> getUpcomingDeliveryMissions(int page, int limit) {
        MutableLiveData<NetworkResultWrapper<List<StaffUpcomingTask>>> result = new MutableLiveData<>();
        result.postValue(NetworkResultWrapper.loading());

        apiService.getUpcomingDeliveryMissions(normalizePage(page), normalizeLimit(limit))
                .enqueue(new Callback<BaseResponse<List<StaffUpcomingDeliveryMissionDto>>>() {
                    @Override
                    public void onResponse(Call<BaseResponse<List<StaffUpcomingDeliveryMissionDto>>> call,
                                           Response<BaseResponse<List<StaffUpcomingDeliveryMissionDto>>> response) {
                        if (!response.isSuccessful()) {
                            result.postValue(NetworkResultWrapper.error(
                                    extractHttpError(response),
                                    response.code()
                            ));
                            return;
                        }

                        BaseResponse<List<StaffUpcomingDeliveryMissionDto>> body = response.body();
                        if (body == null || !body.isSuccess()) {
                            result.postValue(NetworkResultWrapper.error(
                                    body != null ? body.getMessage() : extractHttpError(response)
                            ));
                            return;
                        }

                        result.postValue(NetworkResultWrapper.success(mapDeliveries(body.getData())));
                    }

                    @Override
                    public void onFailure(Call<BaseResponse<List<StaffUpcomingDeliveryMissionDto>>> call, Throwable t) {
                        result.postValue(NetworkResultWrapper.error(safeMessage(t)));
                    }
                });

        return result;
    }

    private List<StaffUpcomingTask> mapDonations(List<StaffUpcomingDonationDto> items) {
        if (items == null || items.isEmpty()) {
            return Collections.emptyList();
        }
        List<StaffUpcomingTask> mapped = new ArrayList<>();
        for (StaffUpcomingDonationDto item : items) {
            if (item == null) {
                continue;
            }
            mapped.add(new StaffUpcomingTask(
                    item.getId(),
                    item.getDonationCode(),
                    item.getName(),
                    item.getPhoneNumber()
            ));
        }
        return mapped;
    }

    private List<StaffUpcomingTask> mapDeliveries(List<StaffUpcomingDeliveryMissionDto> items) {
        if (items == null || items.isEmpty()) {
            return Collections.emptyList();
        }
        List<StaffUpcomingTask> mapped = new ArrayList<>();
        for (StaffUpcomingDeliveryMissionDto item : items) {
            if (item == null) {
                continue;
            }
            mapped.add(new StaffUpcomingTask(
                    item.getId(),
                    item.getMissionCode(),
                    item.getVolunteerName(),
                    item.getVolunteerPhone()
            ));
        }
        return mapped;
    }

    private int normalizePage(int page) {
        return page > 0 ? page : 1;
    }

    private int normalizeLimit(int limit) {
        if (limit <= 0) {
            return 20;
        }
        return Math.min(limit, 100);
    }
}
