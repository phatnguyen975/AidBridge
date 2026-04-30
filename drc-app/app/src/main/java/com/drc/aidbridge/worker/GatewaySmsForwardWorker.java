package com.drc.aidbridge.worker;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.drc.aidbridge.BuildConfig;
import com.drc.aidbridge.data.local.GatewaySmsStatus;
import com.drc.aidbridge.data.local.dao.GatewayPendingSmsDao;
import com.drc.aidbridge.data.local.entity.GatewayPendingSmsEntity;
import com.drc.aidbridge.data.remote.api.gateway.SmsIngestApiService;
import com.drc.aidbridge.data.remote.dto.request.gateway.GatewaySmsSosRequest;
import com.drc.aidbridge.data.remote.dto.response.BaseResponse;
import com.drc.aidbridge.data.remote.dto.response.victim.SosRequestResponse;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import dagger.hilt.android.EntryPointAccessors;
import retrofit2.Response;

public class GatewaySmsForwardWorker extends Worker {

    private static final String TAG = "AidBridgeSmsGateway";
    private static final int BATCH_LIMIT = 20;

    public GatewaySmsForwardWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        SmsFallbackWorkerEntryPoint entryPoint = EntryPointAccessors.fromApplication(
            getApplicationContext(),
            SmsFallbackWorkerEntryPoint.class
        );
        GatewayPendingSmsDao dao = entryPoint.gatewayPendingSmsDao();
        SmsIngestApiService apiService = entryPoint.smsIngestApiService();

        List<GatewayPendingSmsEntity> pendingItems = dao.getByStatuses(
            Arrays.asList(
                GatewaySmsStatus.PARSED,
                GatewaySmsStatus.PENDING_FORWARD,
                GatewaySmsStatus.FORWARD_FAILED
            ),
            BATCH_LIMIT
        );

        boolean shouldRetry = false;
        for (GatewayPendingSmsEntity item : pendingItems) {
            try {
                Log.i(TAG, "SMS_GATEWAY_FORWARD_START clientRequestId=" + item.clientRequestId);
                Response<BaseResponse<SosRequestResponse>> response = apiService
                    .ingestSmsSos(BuildConfig.SMS_GATEWAY_TOKEN, toRequest(item))
                    .execute();

                if (response.isSuccessful()) {
                    BaseResponse<SosRequestResponse> body = response.body();
                    if (body == null || !body.isSuccess()) {
                        dao.markFailed(item.clientRequestId, GatewaySmsStatus.FORWARD_FAILED,
                            System.currentTimeMillis(), body != null ? body.getMessage() : "Empty SMS ingest response");
                        Log.w(TAG, "SMS_GATEWAY_FORWARD_FAILED clientRequestId=" + item.clientRequestId
                            + " error=" + (body != null ? body.getMessage() : "Empty SMS ingest response"));
                        continue;
                    }

                    SosRequestResponse data = body.getData();
                    dao.markForwarded(item.clientRequestId, data != null ? data.getId() : null, System.currentTimeMillis());
                    Log.i(TAG, "SMS_GATEWAY_FORWARD_OK clientRequestId=" + item.clientRequestId);
                    continue;
                }

                if (response.code() >= 500 || response.code() == 408 || response.code() == 429) {
                    shouldRetry = true;
                }
                dao.markFailed(item.clientRequestId, GatewaySmsStatus.FORWARD_FAILED,
                    System.currentTimeMillis(), "HTTP " + response.code() + ": " + response.message());
                Log.w(TAG, "SMS_GATEWAY_FORWARD_FAILED clientRequestId=" + item.clientRequestId
                    + " error=HTTP " + response.code());
            } catch (IOException exception) {
                shouldRetry = true;
                dao.markFailed(item.clientRequestId, GatewaySmsStatus.PENDING_FORWARD,
                    System.currentTimeMillis(), safeMessage(exception));
                Log.w(TAG, "SMS_GATEWAY_PENDING_FORWARD clientRequestId=" + item.clientRequestId
                    + " error=" + safeMessage(exception));
            } catch (Exception exception) {
                dao.markFailed(item.clientRequestId, GatewaySmsStatus.FORWARD_FAILED,
                    System.currentTimeMillis(), safeMessage(exception));
                Log.w(TAG, "SMS_GATEWAY_FORWARD_FAILED clientRequestId=" + item.clientRequestId
                    + " error=" + safeMessage(exception));
            }
        }

        return shouldRetry ? Result.retry() : Result.success();
    }

    private GatewaySmsSosRequest toRequest(GatewayPendingSmsEntity item) {
        return new GatewaySmsSosRequest(
            item.clientRequestId,
            item.senderPhone,
            item.latitude,
            item.longitude,
            item.accuracy,
            item.triggeredAtMillis,
            item.locationCapturedAtMillis,
            item.peopleCount,
            item.quickSos,
            item.rawMessage,
            item.receivedAtGatewayMillis
        );
    }

    private String safeMessage(Throwable throwable) {
        String message = throwable != null ? throwable.getMessage() : null;
        return message == null || message.trim().isEmpty() ? "Gateway SMS forward failed" : message.trim();
    }
}
