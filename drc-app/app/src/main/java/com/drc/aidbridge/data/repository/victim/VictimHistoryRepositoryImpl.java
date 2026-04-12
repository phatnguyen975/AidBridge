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
import com.drc.aidbridge.data.remote.dto.response.victim.HistoryDetailResponse;
import com.drc.aidbridge.data.remote.dto.response.victim.HistoryResponse;
import com.drc.aidbridge.data.repository.BaseRepository;
import com.drc.aidbridge.domain.model.victim.VictimHistoryDetail;
import com.drc.aidbridge.domain.model.victim.VictimHistoryPage;
import com.drc.aidbridge.domain.repository.victim.VictimHistoryRepository;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
                loadCachedPage(normalizedTimeRange, page, size, true, "", 0)
            ));
            return result;
        }

        historyApiService.getVictimHistory(page, size, normalizedTimeRange)
            .enqueue(new Callback<BaseResponse<PaginatedData<HistoryResponse>>>() {
                @Override
                public void onResponse(Call<BaseResponse<PaginatedData<HistoryResponse>>> call,
                                       Response<BaseResponse<PaginatedData<HistoryResponse>>> response) {
                    if (!response.isSuccessful()) {
                        fallbackToCache(result, normalizedTimeRange, page, size,
                            extractHttpError(response), response.code());
                        return;
                    }

                    BaseResponse<PaginatedData<HistoryResponse>> body = response.body();
                    if (body == null) {
                        fallbackToCache(result, normalizedTimeRange, page, size,
                            "Phản hồi lịch sử yêu cầu không hợp lệ.", 0);
                        return;
                    }

                    if (!body.isSuccess()) {
                        String message = body.getMessage();
                        fallbackToCache(result, normalizedTimeRange, page, size,
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
                    fallbackToCache(result, normalizedTimeRange, page, size,
                        "Tải lịch sử yêu cầu thất bại: " + safeMessage(t), 0);
                }
            });

        return result;
    }

    @Override
    public LiveData<NetworkResultWrapper<VictimHistoryDetail>> getVictimHistoryDetail(String requestId,
                                                                                        String type) {
        MutableLiveData<NetworkResultWrapper<VictimHistoryDetail>> result = new MutableLiveData<>();
        result.postValue(NetworkResultWrapper.loading());

        String normalizedRequestId = safeText(requestId);
        String normalizedType = safeText(type);

        if (normalizedRequestId.isEmpty()) {
            result.postValue(NetworkResultWrapper.error("Không tìm thấy mã yêu cầu để tải chi tiết."));
            return result;
        }

        historyApiService.getVictimHistoryDetail(normalizedRequestId, normalizedType)
            .enqueue(new Callback<BaseResponse<HistoryDetailResponse>>() {
                @Override
                public void onResponse(Call<BaseResponse<HistoryDetailResponse>> call,
                                       Response<BaseResponse<HistoryDetailResponse>> response) {
                    if (!response.isSuccessful()) {
                        result.postValue(NetworkResultWrapper.error(extractHttpError(response), response.code()));
                        return;
                    }

                    BaseResponse<HistoryDetailResponse> body = response.body();
                    if (body == null) {
                        result.postValue(NetworkResultWrapper.error("Phản hồi chi tiết yêu cầu không hợp lệ."));
                        return;
                    }

                    if (!body.isSuccess()) {
                        String message = body.getMessage();
                        result.postValue(NetworkResultWrapper.error(
                            message != null && !message.trim().isEmpty()
                                ? message.trim()
                                : "Không thể tải chi tiết yêu cầu."
                        ));
                        return;
                    }

                    HistoryDetailResponse detailResponse = body.getData();
                    if (detailResponse == null) {
                        result.postValue(NetworkResultWrapper.error("Không có dữ liệu chi tiết yêu cầu."));
                        return;
                    }

                    result.postValue(NetworkResultWrapper.success(
                        victimHistoryMapper.mapDetailResponseToDomain(detailResponse)
                    ));
                }

                @Override
                public void onFailure(Call<BaseResponse<HistoryDetailResponse>> call, Throwable t) {
                    result.postValue(NetworkResultWrapper.error(
                        "Tải chi tiết yêu cầu thất bại: " + safeMessage(t)
                    ));
                }
            });

        return result;
    }

    private void fallbackToCache(MutableLiveData<NetworkResultWrapper<VictimHistoryPage>> result,
                                 String timeRange,
                                 int page,
                                 int size,
                                 String fallbackMessage,
                                 int code) {
        ioExecutor.execute(() -> result.postValue(
            loadCachedPage(timeRange, page, size, false, fallbackMessage, code)
        ));
    }

    /**
     * Reads a cached page and maps entities to domain models.
     */
    private NetworkResultWrapper<VictimHistoryPage> loadCachedPage(String timeRange,
                                                                   int page,
                                                                   int size,
                                                                   boolean allowEmptySuccess,
                                                                   String errorMessage,
                                                                   int errorCode) {
        List<VictimHistoryEntity> cachedItems = victimHistoryDao.getPage(timeRange, page);
        boolean hasNextPage;
        if (cachedItems == null || cachedItems.isEmpty()) {
            CachedSlice cachedSlice = loadFilteredCachedSlice(timeRange, page, size);
            if (cachedSlice == null) {
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
            cachedItems = cachedSlice.items;
            hasNextPage = cachedSlice.hasNextPage;
        } else {
            Integer hasNextRaw = victimHistoryDao.getHasNextPage(timeRange, page);
            hasNextPage = hasNextRaw != null && hasNextRaw == 1;
        }

        return NetworkResultWrapper.success(new VictimHistoryPage(
            victimHistoryMapper.mapEntitiesToDomain(cachedItems),
            hasNextPage,
            true
        ));
    }

    private CachedSlice loadFilteredCachedSlice(String timeRange, int page, int size) {
        List<VictimHistoryEntity> allCachedItems = victimHistoryDao.getAllCached();
        if (allCachedItems == null || allCachedItems.isEmpty()) {
            return null;
        }

        List<VictimHistoryEntity> deduplicatedItems = deduplicateByRequestId(allCachedItems);
        List<VictimHistoryEntity> filteredItems = filterByTimeRange(deduplicatedItems, timeRange);

        int safePage = Math.max(1, page);
        int safeSize = Math.max(1, size);
        int fromIndex = (safePage - 1) * safeSize;

        if (filteredItems.isEmpty() || fromIndex >= filteredItems.size()) {
            return new CachedSlice(Collections.emptyList(), false);
        }

        int toIndex = Math.min(fromIndex + safeSize, filteredItems.size());
        return new CachedSlice(
            new ArrayList<>(filteredItems.subList(fromIndex, toIndex)),
            toIndex < filteredItems.size()
        );
    }

    private List<VictimHistoryEntity> deduplicateByRequestId(List<VictimHistoryEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, VictimHistoryEntity> uniqueEntities = new LinkedHashMap<>();
        for (VictimHistoryEntity entity : entities) {
            if (entity == null) {
                continue;
            }

            String requestId = safeText(entity.requestId);
            String dedupeKey = requestId.isEmpty() ? safeText(entity.cacheKey) : requestId;
            if (!uniqueEntities.containsKey(dedupeKey)) {
                uniqueEntities.put(dedupeKey, entity);
            }
        }

        return new ArrayList<>(uniqueEntities.values());
    }

    private List<VictimHistoryEntity> filterByTimeRange(List<VictimHistoryEntity> entities,
                                                         String timeRange) {
        if (entities == null || entities.isEmpty()) {
            return Collections.emptyList();
        }

        Instant fromTime = resolveFromTime(timeRange);
        if (fromTime == null) {
            return entities;
        }

        List<VictimHistoryEntity> filteredItems = new ArrayList<>();
        for (VictimHistoryEntity entity : entities) {
            if (entity == null) {
                continue;
            }

            Instant itemTime = parseHistoryInstant(entity.dateTime, entity.cachedAt);
            if (itemTime != null && !itemTime.isBefore(fromTime)) {
                filteredItems.add(entity);
            }
        }
        return filteredItems;
    }

    private Instant resolveFromTime(String timeRange) {
        Instant now = Instant.now();
        switch (normalizeTimeRange(timeRange)) {
            case "24h":
                return now.minus(Duration.ofHours(24));
            case "7d":
                return now.minus(Duration.ofDays(7));
            case "1m":
                return now.minus(Duration.ofDays(30));
            case "all":
                return null;
            case "1h":
            default:
                return now.minus(Duration.ofHours(1));
        }
    }

    private Instant parseHistoryInstant(String dateTime, long cachedAt) {
        String normalizedDateTime = safeText(dateTime);
        if (!normalizedDateTime.isEmpty()) {
            try {
                return Instant.parse(normalizedDateTime);
            } catch (RuntimeException ignored) {
                // Fall through to cache timestamp for legacy/invalid datetime formats.
            }
        }

        if (cachedAt > 0L) {
            return Instant.ofEpochMilli(cachedAt);
        }

        return null;
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

    private static final class CachedSlice {
        final List<VictimHistoryEntity> items;
        final boolean hasNextPage;

        CachedSlice(List<VictimHistoryEntity> items, boolean hasNextPage) {
            this.items = items != null ? items : Collections.emptyList();
            this.hasNextPage = hasNextPage;
        }
    }
}
