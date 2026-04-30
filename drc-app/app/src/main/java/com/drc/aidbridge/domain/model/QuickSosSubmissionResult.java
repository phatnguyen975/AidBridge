package com.drc.aidbridge.domain.model;

import androidx.annotation.Nullable;

public class QuickSosSubmissionResult {

    public enum DeliveryMode {
        ONLINE,
        SMS,
        PENDING_SYNC
    }

    private final DeliveryMode deliveryMode;
    @Nullable
    private final String serverSosId;
    private final String clientRequestId;
    private final String message;
    @Nullable
    private final String smsBody;
    @Nullable
    private final String gatewayPhoneNumber;
    private final boolean openSmsAppSuggested;

    private QuickSosSubmissionResult(DeliveryMode deliveryMode,
                                     @Nullable String serverSosId,
                                     String clientRequestId,
                                     String message,
                                     @Nullable String smsBody,
                                     @Nullable String gatewayPhoneNumber,
                                     boolean openSmsAppSuggested) {
        this.deliveryMode = deliveryMode;
        this.serverSosId = serverSosId;
        this.clientRequestId = clientRequestId;
        this.message = message;
        this.smsBody = smsBody;
        this.gatewayPhoneNumber = gatewayPhoneNumber;
        this.openSmsAppSuggested = openSmsAppSuggested;
    }

    public static QuickSosSubmissionResult online(String serverSosId, String clientRequestId) {
        return new QuickSosSubmissionResult(
            DeliveryMode.ONLINE,
            serverSosId,
            clientRequestId,
            "SOS online submitted",
            null,
            null,
            false
        );
    }

    public static QuickSosSubmissionResult sentBySms(String clientRequestId,
                                                     String smsBody,
                                                     String gatewayPhoneNumber) {
        return new QuickSosSubmissionResult(
            DeliveryMode.SMS,
            null,
            clientRequestId,
            "No internet. SOS sent by SMS and will sync later.",
            smsBody,
            gatewayPhoneNumber,
            false
        );
    }

    public static QuickSosSubmissionResult pendingSync(String clientRequestId,
                                                       String message,
                                                       @Nullable String smsBody,
                                                       @Nullable String gatewayPhoneNumber,
                                                       boolean openSmsAppSuggested) {
        return new QuickSosSubmissionResult(
            DeliveryMode.PENDING_SYNC,
            null,
            clientRequestId,
            message,
            smsBody,
            gatewayPhoneNumber,
            openSmsAppSuggested
        );
    }

    public DeliveryMode getDeliveryMode() {
        return deliveryMode;
    }

    @Nullable
    public String getServerSosId() {
        return serverSosId;
    }

    public String getClientRequestId() {
        return clientRequestId;
    }

    public String getMessage() {
        return message;
    }

    @Nullable
    public String getSmsBody() {
        return smsBody;
    }

    @Nullable
    public String getGatewayPhoneNumber() {
        return gatewayPhoneNumber;
    }

    public boolean shouldOpenSmsApp() {
        return openSmsAppSuggested;
    }

    public boolean isOnlineCreated() {
        return deliveryMode == DeliveryMode.ONLINE && serverSosId != null && !serverSosId.trim().isEmpty();
    }
}
