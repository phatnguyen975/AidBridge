package com.drc.aidbridge.data.repository.victim;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.drc.aidbridge.data.mapper.victim.VictimHistoryMapper;
import com.drc.aidbridge.data.local.dao.VictimHistoryDao;
import com.drc.aidbridge.data.local.entity.VictimHistoryEntity;
import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.data.remote.api.victim.HistoryApiService;
import com.drc.aidbridge.data.remote.dto.response.BaseResponse;
import com.drc.aidbridge.data.remote.dto.response.PaginatedData;
import com.drc.aidbridge.data.remote.dto.response.victim.HistoryResponse;
import com.drc.aidbridge.data.repository.BaseRepository;
import com.drc.aidbridge.domain.model.victim.VictimHistoryPage;
import com.drc.aidbridge.domain.repository.victim.VictimHistoryRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;
import javax.inject.Singleton;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@Singleton
public class VictimHistoryRepositoryImpl extends BaseRepository implements VictimHistoryRepository {

    private final HistoryApiService historyApiService;
    private final VictimHistoryDao victimHistoryDao;
    private final VictimHistoryMapper victimHistoryMapper;
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();

    @Inject
    public VictimHistoryRepositoryImpl(HistoryApiService historyApiService,
                                       VictimHistoryDao victimHistoryDao,
                                       VictimHistoryMapper victimHistoryMapper) {
        this.historyApiService = historyApiService;
        this.victimHistoryDao = victimHistoryDao;
        this.victimHistoryMapper = victimHistoryMapper;
    }

    /**
     * Remote-first paginated history fetch with Room fallback.
     * When forceOffline=true, the repository reads only local cache.
     */
    @Override
    public LiveData<NetworkResultWrapper<VictimHistoryPage>> getVictimHistory(int page,
                                                                               int size,
                                                                               String timeRange,
                                                                               boolean forceOffline) {
        MutableLiveData<NetworkResultWrapper<VictimHistoryPage>> result = new MutableLiveData<>();
        result.postValue(NetworkResultWrapper.loading());

        String normalizedTimeRange = normalizeTimeRange(timeRange);

        if (forceOffline) {
            ioExecutor.execute(() -> result.postValue(
                loadCachedPage(normalizedTimeRange, page, true, "", 0)
            ));
            return result;
        }

        historyApiService.getVictimHistory(page, size, normalizedTimeRange)
            .enqueue(new Callback<BaseResponse<PaginatedData<HistoryResponse>>>() {
                @Override
                public void onResponse(Call<BaseResponse<PaginatedData<HistoryResponse>>> call,
                                       Response<BaseResponse<PaginatedData<HistoryResponse>>> response) {
                    if (!response.isSuccessful()) {
                        fallbackToCache(result, normalizedTimeRange, page,
                            extractHttpError(response), response.code());
                        return;
                    }

                    BaseResponse<PaginatedData<HistoryResponse>> body = response.body();
                    if (body == null) {
                        fallbackToCache(result, normalizedTimeRange, page,
                            "Phản hồi lịch sử yêu cầu không hợp lệ.", 0);
                        return;
                    }

                    if (!body.isSuccess()) {
                        String message = body.getMessage();
                        fallbackToCache(result, normalizedTimeRange, page,
                            message != null && !message.trim().isEmpty()
                                ? message.trim()
                                : "Không thể tải lịch sử yêu cầu.",
                            0);
                        return;
                    }

                    PaginatedData<HistoryResponse> pageData = body.getData();
                    List<HistoryResponse> items = pageData != null ? pageData.getItems() : Collections.emptyList();
                    boolean hasNextPage = pageData != null && pageData.hasNext();

                    ioExecutor.execute(() -> {
                        List<HistoryResponse> safeItems = sanitizeItems(items);

                        if (page == 1) {
                            victimHistoryDao.clearByTimeRange(normalizedTimeRange);
                        }

                        if (!safeItems.isEmpty()) {
                            victimHistoryDao.insertAll(
                                victimHistoryMapper.mapResponsesToEntities(
                                    safeItems,
                                    normalizedTimeRange,
                                    page,
                                    hasNextPage,
                                    System.currentTimeMillis()
                                )
                            );
                        }

                        result.postValue(NetworkResultWrapper.success(
                            new VictimHistoryPage(
                                victimHistoryMapper.mapResponsesToDomain(safeItems),
                                hasNextPage,
                                false
                            )
                        ));
                    });
                }

                @Override
                public void onFailure(Call<BaseResponse<PaginatedData<HistoryResponse>>> call,
                                      Throwable t) {
                    fallbackToCache(result, normalizedTimeRange, page,
                        "Tải lịch sử yêu cầu thất bại: " + safeMessage(t), 0);
                }
            });

        return result;
    }

    private void fallbackToCache(MutableLiveData<NetworkResultWrapper<VictimHistoryPage>> result,
                                 String timeRange,
                                 int page,
                                 String fallbackMessage,
                                 int code) {
        ioExecutor.execute(() -> result.postValue(
            loadCachedPage(timeRange, page, false, fallbackMessage, code)
        ));
    }

    /**
     * Reads a cached page and maps entities to domain models.
     */
    private NetworkResultWrapper<VictimHistoryPage> loadCachedPage(String timeRange,
                                                                    int page,
                                                                    boolean allowEmptySuccess,
                                                                    String errorMessage,
                                                                    int errorCode) {
        List<VictimHistoryEntity> cachedItems = victimHistoryDao.getPage(timeRange, page);
        if (cachedItems == null || cachedItems.isEmpty()) {
            if (allowEmptySuccess) {
                return NetworkResultWrapper.success(
                    new VictimHistoryPage(Collections.emptyList(), false, true)
                );
            }
            return NetworkResultWrapper.error(
                errorMessage != null && !errorMessage.trim().isEmpty()
                    ? errorMessage.trim()
                    : "Không có dữ liệu lịch sử ngoại tuyến.",
                errorCode
            );
        }

        Integer hasNextRaw = victimHistoryDao.getHasNextPage(timeRange, page);
        boolean hasNextPage = hasNextRaw != null && hasNextRaw == 1;

        return NetworkResultWrapper.success(new VictimHistoryPage(
            victimHistoryMapper.mapEntitiesToDomain(cachedItems),
            hasNextPage,
            true
        ));
    }

    private List<HistoryResponse> sanitizeItems(List<HistoryResponse> items) {
        if (items == null || items.isEmpty()) {
            return Collections.emptyList();
        }

        List<HistoryResponse> safeItems = new ArrayList<>();
        for (HistoryResponse item : items) {
            if (item != null) {
                safeItems.add(item);
            }
        }
        return safeItems;
    }

    private String normalizeTimeRange(String timeRange) {
        String value = safeText(timeRange).toLowerCase(Locale.US);
        switch (value) {
            case "24h":
            case "7d":
            case "1m":
            case "all":
            case "1h":
                return value;
            default:
                return "1h";
        }
    }

    private String safeText(String value) {
        return value != null ? value.trim() : "";
    }
}
