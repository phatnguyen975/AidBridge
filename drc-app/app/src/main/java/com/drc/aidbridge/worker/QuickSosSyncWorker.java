package com.drc.aidbridge.worker;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.drc.aidbridge.data.local.LocalQuickSosStatus;
import com.drc.aidbridge.data.local.dao.LocalQuickSosDao;
import com.drc.aidbridge.data.local.entity.LocalQuickSosEntity;
import com.drc.aidbridge.data.remote.api.victim.SosApiService;
import com.drc.aidbridge.data.remote.dto.request.victim.CreateSosRequest;
import com.drc.aidbridge.data.remote.dto.response.BaseResponse;
import com.drc.aidbridge.data.remote.dto.response.victim.SosRequestResponse;

import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import dagger.hilt.android.EntryPointAccessors;
import retrofit2.Response;

public class QuickSosSyncWorker extends Worker {

    private static final int BATCH_LIMIT = 20;

    public QuickSosSyncWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        SmsFallbackWorkerEntryPoint entryPoint = EntryPointAccessors.fromApplication(
            getApplicationContext(),
            SmsFallbackWorkerEntryPoint.class
        );
        LocalQuickSosDao dao = entryPoint.localQuickSosDao();
        SosApiService apiService = entryPoint.sosApiService();

        List<LocalQuickSosEntity> pendingItems = dao.getByStatuses(
            Arrays.asList(
                LocalQuickSosStatus.PENDING_SYNC,
                LocalQuickSosStatus.SENT_BY_SMS,
                LocalQuickSosStatus.SMS_FAILED,
                LocalQuickSosStatus.FORWARD_FAILED
            ),
            BATCH_LIMIT
        );

        boolean shouldRetry = false;
        for (LocalQuickSosEntity item : pendingItems) {
            try {
                Response<BaseResponse<SosRequestResponse>> response = apiService
                    .createSosRequest(toCreateRequest(item))
                    .execute();

                if (response.isSuccessful()) {
                    BaseResponse<SosRequestResponse> body = response.body();
                    if (body == null || !body.isSuccess()) {
                        dao.markFailed(item.clientRequestId, LocalQuickSosStatus.FORWARD_FAILED,
                            System.currentTimeMillis(), body != null ? body.getMessage() : "Empty SOS sync response");
                        continue;
                    }
                    SosRequestResponse data = body.getData();
                    dao.markSynced(item.clientRequestId, data != null ? data.getId() : null, System.currentTimeMillis());
                    continue;
                }

                if (response.code() >= 500 || response.code() == 408 || response.code() == 429) {
                    shouldRetry = true;
                    dao.markFailed(item.clientRequestId, LocalQuickSosStatus.FORWARD_FAILED,
                        System.currentTimeMillis(), "Retryable HTTP " + response.code());
                    continue;
                }

                dao.markFailed(item.clientRequestId, LocalQuickSosStatus.FORWARD_FAILED,
                    System.currentTimeMillis(), "HTTP " + response.code() + ": " + response.message());
            } catch (IOException exception) {
                shouldRetry = true;
                dao.markFailed(item.clientRequestId, LocalQuickSosStatus.FORWARD_FAILED,
                    System.currentTimeMillis(), safeMessage(exception));
            } catch (Exception exception) {
                dao.markFailed(item.clientRequestId, LocalQuickSosStatus.FORWARD_FAILED,
                    System.currentTimeMillis(), safeMessage(exception));
            }
        }

        if (shouldRetry) {
            return Result.retry();
        }
        return Result.success();
    }

    private CreateSosRequest toCreateRequest(LocalQuickSosEntity item) {
        return CreateSosRequest.createQuickSos(
            item.latitude,
            item.longitude,
            item.accuracy,
            toIsoInstant(item.triggeredAtMillis),
            toIsoInstant(item.locationCapturedAtMillis),
            item.clientRequestId,
            item.deviceInfoJson
        );
    }

    private String toIsoInstant(long timestampMillis) {
        long safeTimestampMillis = timestampMillis > 0L ? timestampMillis : System.currentTimeMillis();
        return Instant.ofEpochMilli(safeTimestampMillis).toString();
    }

    private String safeMessage(Throwable throwable) {
        String message = throwable != null ? throwable.getMessage() : null;
        return message == null || message.trim().isEmpty() ? "Quick SOS sync failed" : message.trim();
    }
}
