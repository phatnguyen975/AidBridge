package com.drc.aidbridge.data.repository.gateway;

import android.content.Context;
import android.util.Log;

import androidx.annotation.Nullable;

import com.drc.aidbridge.BuildConfig;
import com.drc.aidbridge.data.local.GatewaySmsStatus;
import com.drc.aidbridge.data.local.dao.GatewayPendingSmsDao;
import com.drc.aidbridge.data.local.entity.GatewayPendingSmsEntity;
import com.drc.aidbridge.data.remote.api.gateway.SmsIngestApiService;
import com.drc.aidbridge.data.remote.dto.request.gateway.GatewaySmsSosRequest;
import com.drc.aidbridge.data.remote.dto.response.BaseResponse;
import com.drc.aidbridge.data.remote.dto.response.victim.SosRequestResponse;
import com.drc.aidbridge.sms.GatewaySmsSosPayload;
import com.drc.aidbridge.sms.SosSmsFormatter;
import com.drc.aidbridge.sms.SosSmsParser;
import com.drc.aidbridge.utils.NetworkMonitor;
import com.drc.aidbridge.worker.GatewaySmsForwardScheduler;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;
import retrofit2.Response;

@Singleton
public class SmsGatewayRepository {

    private static final String TAG = "AidBridgeSmsGateway";

    private final Context appContext;
    private final SosSmsParser sosSmsParser;
    private final GatewayPendingSmsDao gatewayPendingSmsDao;
    private final SmsIngestApiService smsIngestApiService;
    private final NetworkMonitor networkMonitor;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Inject
    public SmsGatewayRepository(@ApplicationContext Context appContext,
                                SosSmsParser sosSmsParser,
                                GatewayPendingSmsDao gatewayPendingSmsDao,
                                SmsIngestApiService smsIngestApiService,
                                NetworkMonitor networkMonitor) {
        this.appContext = appContext.getApplicationContext();
        this.sosSmsParser = sosSmsParser;
        this.gatewayPendingSmsDao = gatewayPendingSmsDao;
        this.smsIngestApiService = smsIngestApiService;
        this.networkMonitor = networkMonitor;
    }

    public void handleIncomingSms(@Nullable String senderPhone,
                                  @Nullable String rawMessage,
                                  long receivedAtGatewayMillis) {
        handleIncomingSms(senderPhone, rawMessage, receivedAtGatewayMillis, null);
    }

    public void handleIncomingSms(@Nullable String senderPhone,
                                  @Nullable String rawMessage,
                                  long receivedAtGatewayMillis,
                                  @Nullable Runnable completionCallback) {
        if (rawMessage == null || !rawMessage.trim().startsWith(SosSmsFormatter.PREFIX + "|")) {
            Log.i(TAG, "SMS_GATEWAY_IGNORED_INVALID_PREFIX");
            finish(completionCallback);
            return;
        }

        GatewaySmsSosPayload payload = sosSmsParser.parse(senderPhone, rawMessage, receivedAtGatewayMillis);
        if (payload == null) {
            Log.w(TAG, "SMS_GATEWAY_PARSE_FAILED");
            finish(completionCallback);
            return;
        }
        Log.i(TAG, "SMS_GATEWAY_PARSE_OK clientRequestId=" + payload.getClientRequestId());

        executor.execute(() -> {
            try {
                long now = System.currentTimeMillis();
                GatewayPendingSmsEntity entity = new GatewayPendingSmsEntity(
                    payload.getClientRequestId(),
                    payload.getSenderPhone(),
                    payload.getLatitude(),
                    payload.getLongitude(),
                    payload.getAccuracy(),
                    payload.getTriggeredAtMillis(),
                    payload.getLocationCapturedAtMillis(),
                    payload.getPeopleCount(),
                    payload.isQuickSos(),
                    payload.getRawMessage(),
                    payload.getReceivedAtGatewayMillis(),
                    GatewaySmsStatus.PARSED,
                    0,
                    now,
                    now,
                    null
                );
                gatewayPendingSmsDao.insert(entity);

                if (networkMonitor.hasInternet()) {
                    forwardOne(entity);
                } else {
                    gatewayPendingSmsDao.updateStatus(
                        entity.clientRequestId,
                        GatewaySmsStatus.PENDING_FORWARD,
                        System.currentTimeMillis(),
                        "Gateway offline"
                    );
                    Log.i(TAG, "SMS_GATEWAY_PENDING_FORWARD clientRequestId=" + entity.clientRequestId);
                    enqueueForwardWorker();
                }
            } finally {
                finish(completionCallback);
            }
        });
    }

    public int countAll() {
        return gatewayPendingSmsDao.countAll();
    }

    public int countPendingForward() {
        return gatewayPendingSmsDao.countByStatuses(Arrays.asList(
            GatewaySmsStatus.PARSED,
            GatewaySmsStatus.PENDING_FORWARD,
            GatewaySmsStatus.FORWARD_FAILED
        ));
    }

    @Nullable
    public String getLatestError() {
        return gatewayPendingSmsDao.getLatestError();
    }

    private void forwardOne(GatewayPendingSmsEntity entity) {
        try {
            Log.i(TAG, "SMS_GATEWAY_FORWARD_START clientRequestId=" + entity.clientRequestId);
            Response<BaseResponse<SosRequestResponse>> response = smsIngestApiService
                .ingestSmsSos(BuildConfig.SMS_GATEWAY_TOKEN, toRequest(entity))
                .execute();

            if (response.isSuccessful()) {
                BaseResponse<SosRequestResponse> body = response.body();
                if (body != null && body.isSuccess()) {
                    SosRequestResponse data = body.getData();
                    gatewayPendingSmsDao.markForwarded(
                        entity.clientRequestId,
                        data != null ? data.getId() : null,
                        System.currentTimeMillis()
                    );
                    Log.i(TAG, "SMS_GATEWAY_FORWARD_OK clientRequestId=" + entity.clientRequestId);
                    return;
                }
            }

            String errorMessage = "HTTP " + response.code();
            gatewayPendingSmsDao.markFailed(
                entity.clientRequestId,
                GatewaySmsStatus.FORWARD_FAILED,
                System.currentTimeMillis(),
                errorMessage
            );
            Log.w(TAG, "SMS_GATEWAY_FORWARD_FAILED clientRequestId=" + entity.clientRequestId
                + " error=" + errorMessage);
            if (response.code() >= 500 || response.code() == 408 || response.code() == 429) {
                enqueueForwardWorker();
            }
        } catch (IOException exception) {
            gatewayPendingSmsDao.markFailed(entity.clientRequestId, GatewaySmsStatus.PENDING_FORWARD,
                System.currentTimeMillis(), safeMessage(exception));
            Log.w(TAG, "SMS_GATEWAY_PENDING_FORWARD clientRequestId=" + entity.clientRequestId
                + " error=" + safeMessage(exception));
            enqueueForwardWorker();
        } catch (Exception exception) {
            gatewayPendingSmsDao.markFailed(entity.clientRequestId, GatewaySmsStatus.FORWARD_FAILED,
                System.currentTimeMillis(), safeMessage(exception));
            Log.w(TAG, "SMS_GATEWAY_FORWARD_FAILED clientRequestId=" + entity.clientRequestId
                + " error=" + safeMessage(exception));
        }
    }

    private GatewaySmsSosRequest toRequest(GatewayPendingSmsEntity entity) {
        return new GatewaySmsSosRequest(
            entity.clientRequestId,
            entity.senderPhone,
            entity.latitude,
            entity.longitude,
            entity.accuracy,
            entity.triggeredAtMillis,
            entity.locationCapturedAtMillis,
            entity.peopleCount,
            entity.quickSos,
            entity.rawMessage,
            entity.receivedAtGatewayMillis
        );
    }

    private String safeMessage(Throwable throwable) {
        String message = throwable != null ? throwable.getMessage() : null;
        return message == null || message.trim().isEmpty() ? "Gateway forward failed" : message.trim();
    }

    private void enqueueForwardWorker() {
        GatewaySmsForwardScheduler.enqueue(appContext);
        Log.i(TAG, "SMS_GATEWAY_WORKER_ENQUEUED");
    }

    private void finish(@Nullable Runnable completionCallback) {
        if (completionCallback == null) {
            return;
        }
        try {
            completionCallback.run();
        } catch (Exception ignored) {
        }
    }
}
