package com.drc.aidbridge.data.repository.victim;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.drc.aidbridge.BuildConfig;
import com.drc.aidbridge.data.local.LocalQuickSosStatus;
import com.drc.aidbridge.data.local.dao.LocalQuickSosDao;
import com.drc.aidbridge.data.local.entity.LocalQuickSosEntity;
import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.data.remote.api.victim.SosApiService;
import com.drc.aidbridge.data.remote.dto.request.victim.CreateSosRequest;
import com.drc.aidbridge.data.remote.dto.response.BaseResponse;
import com.drc.aidbridge.data.remote.dto.response.victim.SosRequestResponse;
import com.drc.aidbridge.data.repository.BaseRepository;
import com.drc.aidbridge.domain.model.QuickSosSubmissionResult;
import com.drc.aidbridge.domain.repository.victim.VictimSosRepository;
import com.drc.aidbridge.sms.SosSmsFormatter;
import com.drc.aidbridge.sms.SosSmsSender;
import com.drc.aidbridge.utils.NetworkMonitor;
import com.drc.aidbridge.utils.TokenManager;
import com.drc.aidbridge.worker.QuickSosSyncScheduler;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.time.Instant;
import java.text.Normalizer;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@Singleton
public class VictimSosRepositoryImpl extends BaseRepository implements VictimSosRepository {

    private static final String SMS_TAG = "AidBridgeSmsFallback";

    private final Context appContext;
    private final SosApiService sosApiService;
    private final TokenManager tokenManager;
    private final NetworkMonitor networkMonitor;
    private final LocalQuickSosDao localQuickSosDao;
    private final SosSmsFormatter sosSmsFormatter;
    private final SosSmsSender sosSmsSender;
    private final ExecutorService smsFallbackExecutor = Executors.newSingleThreadExecutor();

    @Inject
    public VictimSosRepositoryImpl(@ApplicationContext Context appContext,
                                   SosApiService sosApiService,
                                   TokenManager tokenManager,
                                   NetworkMonitor networkMonitor,
                                   LocalQuickSosDao localQuickSosDao,
                                   SosSmsFormatter sosSmsFormatter,
                                   SosSmsSender sosSmsSender) {
        this.appContext = appContext.getApplicationContext();
        this.sosApiService = sosApiService;
        this.tokenManager = tokenManager;
        this.networkMonitor = networkMonitor;
        this.localQuickSosDao = localQuickSosDao;
        this.sosSmsFormatter = sosSmsFormatter;
        this.sosSmsSender = sosSmsSender;
    }

    @Override
    public LiveData<NetworkResultWrapper<QuickSosSubmissionResult>> uploadQuickSelfSos(double latitude,
                                                                                       double longitude,
                                                                                       Double accuracy,
                                                                                       long triggeredAtMillis,
                                                                                       long locationCapturedAtMillis,
                                                                                       String clientRequestId,
                                                                                       String deviceInfo) {
        MutableLiveData<NetworkResultWrapper<QuickSosSubmissionResult>> result = new MutableLiveData<>();
        result.postValue(NetworkResultWrapper.loading());
        String safeClientRequestId = safeTrim(clientRequestId);

        CreateSosRequest request = CreateSosRequest.createQuickSos(
            latitude,
            longitude,
            accuracy,
            toIsoInstant(triggeredAtMillis),
            toIsoInstant(locationCapturedAtMillis),
            safeClientRequestId,
            safeTrim(deviceInfo)
        );

        String offlineReason = getOfflineFallbackReason();
        if (offlineReason != null) {
            fallbackQuickSosToSms(
                result,
                latitude,
                longitude,
                accuracy,
                triggeredAtMillis,
                locationCapturedAtMillis,
                safeClientRequestId,
                deviceInfo,
                offlineReason
            );
            return result;
        }

        sosApiService.createSosRequest(request).enqueue(new Callback<BaseResponse<SosRequestResponse>>() {
            @Override
            public void onResponse(Call<BaseResponse<SosRequestResponse>> call,
                                   Response<BaseResponse<SosRequestResponse>> response) {
                if (!response.isSuccessful()) {
                    result.postValue(NetworkResultWrapper.error(extractHttpError(response), response.code()));
                    return;
                }

                BaseResponse<SosRequestResponse> baseResponse = response.body();
                if (baseResponse == null) {
                    result.postValue(NetworkResultWrapper.error("Phản hồi gửi SOS không hợp lệ."));
                    return;
                }

                if (!baseResponse.isSuccess()) {
                    result.postValue(NetworkResultWrapper.error(
                        firstNonBlank(baseResponse.getMessage(), "Gửi SOS thất bại.")
                    ));
                    return;
                }

                SosRequestResponse responseData = baseResponse.getData();
                String sosId = responseData != null ? safeTrim(responseData.getId()) : "";
                if (sosId.isEmpty()) {
                    result.postValue(NetworkResultWrapper.error(
                        "Phản hồi SOS thiếu mã yêu cầu để theo dõi vị trí."
                    ));
                    return;
                }

                result.postValue(NetworkResultWrapper.success(
                    QuickSosSubmissionResult.online(sosId, safeClientRequestId)
                ));
            }

            @Override
            public void onFailure(Call<BaseResponse<SosRequestResponse>> call, Throwable t) {
                if (isNetworkFailure(t)) {
                    fallbackQuickSosToSms(
                        result,
                        latitude,
                        longitude,
                        accuracy,
                        triggeredAtMillis,
                        locationCapturedAtMillis,
                        safeClientRequestId,
                        deviceInfo,
                        safeMessage(t)
                    );
                    return;
                }
                result.postValue(NetworkResultWrapper.error("Gửi SOS thất bại: " + safeMessage(t)));
            }
        });

        return result;
    }

    @Override
    public LiveData<NetworkResultWrapper<String>> uploadSelfSos(String fullName,
                                                                 int peopleCount,
                                                                 String severity,
                                                                 String note,
                                                                 double latitude,
                                                                 double longitude,
                                                                 String firstImageUrl) {
        MutableLiveData<NetworkResultWrapper<String>> result = new MutableLiveData<>();
        result.postValue(NetworkResultWrapper.loading());

        String accountName = firstNonBlank(safeTrim(tokenManager.getUserName()), safeTrim(fullName));
        String accountPhone = safeTrim(tokenManager.getUserPhone());
        String description = buildSelfDescription(accountName, accountPhone, note);

        CreateSosRequest request = new CreateSosRequest(
            latitude,
            longitude,
            null,
            description,
            peopleCount,
            mapSeverityToUrgencyLevel(severity),
            safeTrim(firstImageUrl)
        );

        String offlineReason = getOfflineFallbackReason();
        if (offlineReason != null) {
            fallbackSelfSosToSms(
                result,
                accountName,
                peopleCount,
                note,
                latitude,
                longitude,
                offlineReason
            );
            return result;
        }

        sosApiService.createSosRequest(request).enqueue(new Callback<BaseResponse<SosRequestResponse>>() {
            @Override
            public void onResponse(Call<BaseResponse<SosRequestResponse>> call,
                                   Response<BaseResponse<SosRequestResponse>> response) {
                if (!response.isSuccessful()) {
                    result.postValue(NetworkResultWrapper.error(extractHttpError(response), response.code()));
                    return;
                }

                BaseResponse<SosRequestResponse> baseResponse = response.body();
                if (baseResponse == null) {
                    result.postValue(NetworkResultWrapper.error("Phản hồi gửi SOS không hợp lệ."));
                    return;
                }

                if (!baseResponse.isSuccess()) {
                    result.postValue(NetworkResultWrapper.error(
                        firstNonBlank(baseResponse.getMessage(), "Gửi SOS thất bại.")
                    ));
                    return;
                }

                String successMessage = firstNonBlank(
                    baseResponse.getMessage(),
                    "Gửi SOS thành công."
                );
                result.postValue(NetworkResultWrapper.success(successMessage));
            }

            @Override
            public void onFailure(Call<BaseResponse<SosRequestResponse>> call, Throwable t) {
                if (isNetworkFailure(t)) {
                    fallbackSelfSosToSms(
                        result,
                        accountName,
                        peopleCount,
                        note,
                        latitude,
                        longitude,
                        safeMessage(t)
                    );
                    return;
                }
                result.postValue(NetworkResultWrapper.error("Gửi SOS thất bại: " + safeMessage(t)));
            }
        });

        return result;
    }

    @Override
    public LiveData<NetworkResultWrapper<String>> uploadRelativeSos(String relativeName,
                                                                     String relativePhone,
                                                                     String relativeAddress,
                                                                     String severity,
                                                                     double latitude,
                                                                     double longitude) {
        MutableLiveData<NetworkResultWrapper<String>> result = new MutableLiveData<>();
        result.postValue(NetworkResultWrapper.loading());

        String description = buildRelativeDescription(relativeName, relativePhone);
        CreateSosRequest request = new CreateSosRequest(
            latitude,
            longitude,
            safeTrim(relativeAddress),
            description,
            1,
            mapSeverityToUrgencyLevel(severity),
            null
        );

        sosApiService.createSosRequest(request).enqueue(new Callback<BaseResponse<SosRequestResponse>>() {
            @Override
            public void onResponse(Call<BaseResponse<SosRequestResponse>> call,
                                   Response<BaseResponse<SosRequestResponse>> response) {
                if (!response.isSuccessful()) {
                    result.postValue(NetworkResultWrapper.error(extractHttpError(response), response.code()));
                    return;
                }

                BaseResponse<SosRequestResponse> baseResponse = response.body();
                if (baseResponse == null) {
                    result.postValue(NetworkResultWrapper.error("Phản hồi gửi SOS cho người thân không hợp lệ."));
                    return;
                }

                if (!baseResponse.isSuccess()) {
                    result.postValue(NetworkResultWrapper.error(
                        firstNonBlank(baseResponse.getMessage(), "Gửi SOS cho người thân thất bại.")
                    ));
                    return;
                }

                String successMessage = firstNonBlank(
                    baseResponse.getMessage(),
                    "Gửi SOS cho người thân thành công."
                );
                result.postValue(NetworkResultWrapper.success(successMessage));
            }

            @Override
            public void onFailure(Call<BaseResponse<SosRequestResponse>> call, Throwable t) {
                result.postValue(NetworkResultWrapper.error(
                    "Gửi SOS cho người thân thất bại: " + safeMessage(t)
                ));
            }
        });

        return result;
    }

    private String buildRelativeDescription(String relativeName, String relativePhone) {
        String name = firstNonBlank(safeTrim(relativeName), "Không có");
        String phone = firstNonBlank(safeTrim(relativePhone), "Không có");
        return "Loại yêu cầu: SOS người thân. Họ tên: " + name + ". Số điện thoại liên hệ: " + phone;
    }

    private String buildSelfDescription(String fullName, String phoneNumber, String healthDetail) {
        String name = firstNonBlank(safeTrim(fullName), "Không có");
        String phone = firstNonBlank(safeTrim(phoneNumber), "Không có");
        String health = firstNonBlank(safeTrim(healthDetail), "Không có");

        return "Loại yêu cầu: SOS bản thân. Họ tên: " + name
            + ". Số điện thoại liên hệ: " + phone
            + ". Chi tiết sức khỏe: " + health;
    }

    private String mapSeverityToUrgencyLevel(String severity) {
        String value = normalizeSeverityText(severity);
        if (value.isEmpty()) {
            return "CRITICAL";
        }

        if ("CRITICAL".equals(value)
            || "NGUY KICH".equals(value)
            || "NGUYKICH".equals(value)) {
            return "CRITICAL";
        }
        if ("HIGH".equals(value)
            || "NGHIEM TRONG".equals(value)
            || "NGHIEMTRONG".equals(value)) {
            return "HIGH";
        }
        if ("LOW".equals(value) || "NHE".equals(value)) {
            return "LOW";
        }
        if ("MEDIUM".equals(value)
            || "TRUNG BINH".equals(value)
            || "TRUNGBINH".equals(value)) {
            return "MEDIUM";
        }

        return "CRITICAL";
    }

    private String normalizeSeverityText(String value) {
        String trimmed = safeTrim(value);
        if (trimmed.isEmpty()) {
            return "";
        }

        String normalized = Normalizer.normalize(trimmed, Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "")
            .toUpperCase(Locale.US)
            .trim();

        return normalized;
    }

    private String safeTrim(String value) {
        return value != null ? value.trim() : "";
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }

        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return "";
    }

    private String toIsoInstant(long timestampMillis) {
        long safeTimestampMillis = timestampMillis > 0L ? timestampMillis : System.currentTimeMillis();
        return Instant.ofEpochMilli(safeTimestampMillis).toString();
    }

    private void fallbackQuickSosToSms(MutableLiveData<NetworkResultWrapper<QuickSosSubmissionResult>> result,
                                       double latitude,
                                       double longitude,
                                       Double accuracy,
                                       long triggeredAtMillis,
                                       long locationCapturedAtMillis,
                                       String clientRequestId,
                                       String deviceInfo,
                                       String reason) {
        String safeClientRequestId = safeTrim(clientRequestId);
        String gatewayPhoneNumber = safeTrim(BuildConfig.SOS_GATEWAY_PHONE_NUMBER);
        long safeTriggeredAtMillis = triggeredAtMillis > 0L ? triggeredAtMillis : System.currentTimeMillis();
        long safeLocationCapturedAtMillis = locationCapturedAtMillis > 0L
            ? locationCapturedAtMillis
            : safeTriggeredAtMillis;

        Log.i(SMS_TAG, "SMS_FALLBACK_START clientRequestId=" + safeClientRequestId + " reason=" + safeTrim(reason));
        String smsBody = sosSmsFormatter.formatQuickSos(
            safeClientRequestId,
            latitude,
            longitude,
            accuracy,
            safeTriggeredAtMillis,
            1,
            true,
            safeTrim(tokenManager.getUserPhone()),
            null
        );
        Log.i(SMS_TAG, "SMS_BODY_BUILT clientRequestId=" + safeClientRequestId + " length=" + smsBody.length());

        smsFallbackExecutor.execute(() -> {
            long now = System.currentTimeMillis();
            LocalQuickSosEntity entity = new LocalQuickSosEntity(
                safeClientRequestId,
                latitude,
                longitude,
                accuracy,
                safeTriggeredAtMillis,
                safeLocationCapturedAtMillis,
                safeTrim(deviceInfo),
                smsBody,
                gatewayPhoneNumber,
                LocalQuickSosStatus.PENDING_SMS,
                0,
                now,
                now,
                safeTrim(reason)
            );
            localQuickSosDao.insert(entity);
            Log.i(SMS_TAG, "SMS_LOCAL_QUEUED clientRequestId=" + safeClientRequestId
                + " status=" + LocalQuickSosStatus.PENDING_SMS);

            if (!sosSmsSender.canAttemptSms()) {
                Log.w(SMS_TAG, "SMS_DEVICE_NOT_SUPPORTED clientRequestId=" + safeClientRequestId);
                markPendingSyncAndReturn(result, safeClientRequestId, smsBody, gatewayPhoneNumber,
                    "Thiết bị không hỗ trợ SMS. SOS đã được lưu và sẽ tự đồng bộ khi có Internet.",
                    false);
                return;
            }

            if (!sosSmsSender.hasSendSmsPermission()) {
                Log.w(SMS_TAG, "SMS_PERMISSION_DENIED clientRequestId=" + safeClientRequestId);
                markPendingSyncAndReturn(result, safeClientRequestId, smsBody, gatewayPhoneNumber,
                    "Chưa có quyền gửi SMS. SOS đã được lưu và sẽ tự đồng bộ khi có Internet.",
                    !gatewayPhoneNumber.isEmpty());
                return;
            }

            Log.i(SMS_TAG, "SMS_SEND_REQUESTED clientRequestId=" + safeClientRequestId);
            sosSmsSender.send(gatewayPhoneNumber, smsBody, safeClientRequestId, sendResult ->
                smsFallbackExecutor.execute(() -> {
                    if (sendResult.isSuccess()) {
                        localQuickSosDao.updateStatus(
                            safeClientRequestId,
                            LocalQuickSosStatus.SENT_BY_SMS,
                            System.currentTimeMillis(),
                            null
                        );
                        Log.i(SMS_TAG, "SMS_SENT_OK clientRequestId=" + safeClientRequestId);
                        enqueueQuickSosSync();
                        result.postValue(NetworkResultWrapper.success(
                            QuickSosSubmissionResult.sentBySms(safeClientRequestId, smsBody, gatewayPhoneNumber)
                        ));
                        return;
                    }

                    String status = sendResult.isPermissionMissing()
                        ? LocalQuickSosStatus.PENDING_SYNC
                        : LocalQuickSosStatus.SMS_FAILED;
                    localQuickSosDao.markFailed(
                        safeClientRequestId,
                        status,
                        System.currentTimeMillis(),
                        sendResult.getErrorMessage()
                    );
                    Log.w(SMS_TAG, "SMS_SENT_FAILED clientRequestId=" + safeClientRequestId
                        + " status=" + status
                        + " error=" + sendResult.getErrorMessage());
                    enqueueQuickSosSync();
                    String failureMessage = firstNonBlank(
                        sendResult.getErrorMessage(),
                        "Không gửi được SMS. SOS đã được lưu và sẽ tự đồng bộ khi có Internet."
                    );
                    result.postValue(NetworkResultWrapper.success(
                        QuickSosSubmissionResult.pendingSync(
                            safeClientRequestId,
                            failureMessage,
                            smsBody,
                            gatewayPhoneNumber,
                            sendResult.isPermissionMissing() && !gatewayPhoneNumber.isEmpty()
                        )
                    ));
                })
            );
        });
    }

    private void markPendingSyncAndReturn(MutableLiveData<NetworkResultWrapper<QuickSosSubmissionResult>> result,
                                          String clientRequestId,
                                          String smsBody,
                                          String gatewayPhoneNumber,
                                          String message,
                                          boolean openSmsAppSuggested) {
        localQuickSosDao.updateStatus(
            clientRequestId,
            LocalQuickSosStatus.PENDING_SYNC,
            System.currentTimeMillis(),
            message
        );
        enqueueQuickSosSync();
        result.postValue(NetworkResultWrapper.success(
            QuickSosSubmissionResult.pendingSync(
                clientRequestId,
                message,
                smsBody,
                gatewayPhoneNumber,
                openSmsAppSuggested
            )
        ));
    }

    private void fallbackSelfSosToSms(MutableLiveData<NetworkResultWrapper<String>> result,
                                      String fullName,
                                      int peopleCount,
                                      String note,
                                      double latitude,
                                      double longitude,
                                      String reason) {
        String clientRequestId = UUID.randomUUID().toString();
        String gatewayPhoneNumber = safeTrim(BuildConfig.SOS_GATEWAY_PHONE_NUMBER);
        long now = System.currentTimeMillis();
        String senderPhone = safeTrim(tokenManager.getUserPhone());
        String smsBody = sosSmsFormatter.formatQuickSos(
            clientRequestId,
            latitude,
            longitude,
            null,
            now,
            Math.max(1, peopleCount),
            false,
            senderPhone,
            buildSelfDescription(fullName, senderPhone, note)
        );

        Log.i(SMS_TAG, "SMS_SELF_FALLBACK_START clientRequestId=" + clientRequestId
            + " reason=" + safeTrim(reason));
        Log.i(SMS_TAG, "SMS_SELF_BODY_BUILT clientRequestId=" + clientRequestId
            + " length=" + smsBody.length());

        smsFallbackExecutor.execute(() -> {
            if (!sosSmsSender.canAttemptSms()) {
                Log.w(SMS_TAG, "SMS_SELF_DEVICE_NOT_SUPPORTED clientRequestId=" + clientRequestId);
                result.postValue(NetworkResultWrapper.error(
                    "Thiết bị không hỗ trợ SMS. Không thể gửi SOS khi không kết nối được server."
                ));
                return;
            }

            if (!sosSmsSender.hasSendSmsPermission()) {
                Log.w(SMS_TAG, "SMS_SELF_PERMISSION_DENIED clientRequestId=" + clientRequestId);
                result.postValue(NetworkResultWrapper.error(
                    "Chưa có quyền gửi SMS. Hãy cấp quyền SMS để gửi SOS qua staff gateway khi offline."
                ));
                return;
            }

            Log.i(SMS_TAG, "SMS_SELF_SEND_REQUESTED clientRequestId=" + clientRequestId);
            sosSmsSender.send(gatewayPhoneNumber, smsBody, clientRequestId, sendResult -> {
                if (sendResult.isSuccess()) {
                    Log.i(SMS_TAG, "SMS_SELF_SENT_OK clientRequestId=" + clientRequestId);
                    result.postValue(NetworkResultWrapper.success(
                        "Không kết nối được server. SOS đã được gửi qua SMS gateway."
                    ));
                    return;
                }

                Log.w(SMS_TAG, "SMS_SELF_SENT_FAILED clientRequestId=" + clientRequestId
                    + " error=" + sendResult.getErrorMessage());
                result.postValue(NetworkResultWrapper.error(firstNonBlank(
                    sendResult.getErrorMessage(),
                    "Không gửi được SMS tới staff gateway."
                )));
            });
        });
    }

    private void enqueueQuickSosSync() {
        QuickSosSyncScheduler.enqueue(appContext);
        Log.i(SMS_TAG, "SMS_SYNC_WORKER_ENQUEUED");
    }

    private String getOfflineFallbackReason() {
        if (!networkMonitor.hasInternet()) {
            return "Device offline";
        }
        if (isPrivateLanBackendUrl(BuildConfig.BASE_URL) && !networkMonitor.hasLocalNetworkTransport()) {
            return "Backend LAN URL is not reachable without Wi-Fi";
        }
        return null;
    }

    private boolean isPrivateLanBackendUrl(String baseUrl) {
        String value = safeTrim(baseUrl).toLowerCase(Locale.US);
        if (value.isEmpty()) {
            return false;
        }

        String host;
        try {
            host = new URI(value).getHost();
        } catch (URISyntaxException ignored) {
            host = null;
        }
        if (host == null || host.trim().isEmpty()) {
            host = value
                .replaceFirst("^https?://", "")
                .split("[/:]", 2)[0];
        }

        return isPrivateLanHost(host);
    }

    private boolean isPrivateLanHost(String host) {
        String value = safeTrim(host).toLowerCase(Locale.US);
        if (value.startsWith("192.168.")) {
            return true;
        }
        if (value.startsWith("172.")) {
            String[] parts = value.split("\\.");
            if (parts.length < 2) {
                return false;
            }
            try {
                int secondOctet = Integer.parseInt(parts[1]);
                return secondOctet >= 16 && secondOctet <= 31;
            } catch (NumberFormatException ignored) {
                return false;
            }
        }
        return false;
    }

    private boolean isNetworkFailure(Throwable throwable) {
        return throwable instanceof IOException
            || throwable instanceof SocketTimeoutException
            || throwable instanceof InterruptedIOException
            || throwable instanceof UnknownHostException
            || throwable instanceof ConnectException
            || throwable instanceof NoRouteToHostException;
    }
}
