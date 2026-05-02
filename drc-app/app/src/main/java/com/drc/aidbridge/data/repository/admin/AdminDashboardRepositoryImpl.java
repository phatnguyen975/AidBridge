package com.drc.aidbridge.data.repository.admin;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.data.remote.api.admin.AdminDashboardApiService;
import com.drc.aidbridge.data.remote.dto.response.BaseResponse;
import com.drc.aidbridge.data.remote.dto.response.admin.AdminDashboardSummaryResponseDto;
import com.drc.aidbridge.data.remote.dto.response.admin.AdminRoutingSosAidResponseDto;
import com.drc.aidbridge.data.repository.BaseRepository;
import com.drc.aidbridge.domain.model.admin.AdminDashboardSummary;
import com.drc.aidbridge.domain.repository.admin.AdminDashboardRepository;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@Singleton
public class AdminDashboardRepositoryImpl extends BaseRepository implements AdminDashboardRepository {

    private final AdminDashboardApiService apiService;

    @Inject
    public AdminDashboardRepositoryImpl(AdminDashboardApiService apiService) {
        this.apiService = apiService;
    }

    @Override
    public LiveData<NetworkResultWrapper<AdminDashboardSummary>> getSummary() {
        MutableLiveData<NetworkResultWrapper<AdminDashboardSummary>> result = new MutableLiveData<>();
        result.postValue(NetworkResultWrapper.loading());

        apiService.getSummary().enqueue(new Callback<BaseResponse<AdminDashboardSummaryResponseDto>>() {
            @Override
            public void onResponse(Call<BaseResponse<AdminDashboardSummaryResponseDto>> call,
                                   Response<BaseResponse<AdminDashboardSummaryResponseDto>> response) {
                if (!response.isSuccessful()) {
                    result.postValue(NetworkResultWrapper.error(extractHttpError(response), response.code()));
                    return;
                }

                BaseResponse<AdminDashboardSummaryResponseDto> baseResponse = response.body();
                if (baseResponse == null) {
                    result.postValue(NetworkResultWrapper.error("Phản hồi dashboard không hợp lệ."));
                    return;
                }

                if (!baseResponse.isSuccess()) {
                    String message = baseResponse.getMessage();
                    result.postValue(NetworkResultWrapper.error(
                            message != null && !message.trim().isEmpty()
                                    ? message
                                    : "Không thể tải dashboard."));
                    return;
                }

                AdminDashboardSummary summary = mapToDomain(baseResponse.getData());
                if (summary == null) {
                    result.postValue(NetworkResultWrapper.error("Dữ liệu dashboard không hợp lệ."));
                    return;
                }

                result.postValue(NetworkResultWrapper.success(summary));
            }

            @Override
            public void onFailure(Call<BaseResponse<AdminDashboardSummaryResponseDto>> call, Throwable throwable) {
                result.postValue(NetworkResultWrapper.error(
                        "Không thể kết nối máy chủ: " + safeMessage(throwable)));
            }
        });

        return result;
    }

    @Override
    public LiveData<NetworkResultWrapper<AdminRoutingSosAidResponseDto>> getSosAidRequests(String status, String startDate, String endDate) {
        MutableLiveData<NetworkResultWrapper<AdminRoutingSosAidResponseDto>> result = new MutableLiveData<>();
        result.postValue(NetworkResultWrapper.loading());

        apiService.getSosAidRequests(status, startDate, endDate).enqueue(new Callback<AdminRoutingSosAidResponseDto>() {
            @Override
            public void onResponse(Call<AdminRoutingSosAidResponseDto> call,
                                   Response<AdminRoutingSosAidResponseDto> response) {
                if (!response.isSuccessful()) {
                    result.postValue(NetworkResultWrapper.error(extractHttpError(response), response.code()));
                    return;
                }

                AdminRoutingSosAidResponseDto body = response.body();
                if (body == null) {
                    result.postValue(NetworkResultWrapper.error("Phản hồi SOS/Aid không hợp lệ."));
                    return;
                }

                result.postValue(NetworkResultWrapper.success(body));
            }

            @Override
            public void onFailure(Call<AdminRoutingSosAidResponseDto> call, Throwable throwable) {
                result.postValue(NetworkResultWrapper.error(
                        "Không thể kết nối máy chủ: " + safeMessage(throwable)));
            }
        });

        return result;
    }

    private AdminDashboardSummary mapToDomain(AdminDashboardSummaryResponseDto dto) {
        if (dto == null) {
            return null;
        }

        List<AdminDashboardSummary.ItemCategoryStat> stats = new ArrayList<>();
        List<AdminDashboardSummaryResponseDto.ItemCategoryStatDto> dtoStats = dto.getItemCategoryStats();
        if (dtoStats != null) {
            for (AdminDashboardSummaryResponseDto.ItemCategoryStatDto statDto : dtoStats) {
                if (statDto == null) {
                    continue;
                }
                stats.add(new AdminDashboardSummary.ItemCategoryStat(
                        safeText(statDto.getCategory()),
                        safeLong(statDto.getQuantity())
                ));
            }
        }

        List<AdminDashboardSummary.AdminAlert> alerts = new ArrayList<>();
        List<AdminDashboardSummaryResponseDto.AlertDto> alertDtos = dto.getAlerts();
        if (alertDtos != null) {
            for (AdminDashboardSummaryResponseDto.AlertDto alertDto : alertDtos) {
                if (alertDto == null) {
                    continue;
                }
                alerts.add(new AdminDashboardSummary.AdminAlert(
                        safeText(alertDto.getId()),
                        safeText(alertDto.getTitle()),
                        safeText(alertDto.getMessage()),
                        safeText(alertDto.getSeverity())
                ));
            }
        }

        List<AdminDashboardSummary.RecentActivity> activities = new ArrayList<>();
        List<AdminDashboardSummaryResponseDto.RecentActivityDto> activityDtos = dto.getRecentActivities();
        if (activityDtos != null) {
            for (AdminDashboardSummaryResponseDto.RecentActivityDto activityDto : activityDtos) {
                if (activityDto == null) {
                    continue;
                }
                activities.add(new AdminDashboardSummary.RecentActivity(
                        safeText(activityDto.getId()),
                        safeText(activityDto.getTitle()),
                        safeText(activityDto.getDescription()),
                        safeText(activityDto.getType()),
                        safeText(activityDto.getCreatedAt())
                ));
            }
        }

        return new AdminDashboardSummary(
                safeLong(dto.getTotalHubs()),
                safeLong(dto.getTotalVolunteers()),
                safeLong(dto.getTodayMissions()),
                safeLong(dto.getDistributedItems()),
                stats,
                safeLong(dto.getTotalPackages()),
                safeDouble(dto.getPackageGrowthPercent()),
                safeLong(dto.getTotalPeopleSupported()),
                alerts,
                activities
        );
    }

    private long safeLong(Long value) {
        if (value == null || value < 0L) {
            return 0L;
        }
        return value;
    }

    private String safeText(String value) {
        return value == null ? "" : value.trim();
    }

    private double safeDouble(Double value) {
        if (value == null || value.isNaN() || value.isInfinite()) {
            return 0d;
        }
        return value;
    }
}
