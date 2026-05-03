package com.drc.aidbridge.worker;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

public final class GatewaySmsForwardScheduler {

    private static final String GATEWAY_SMS_FORWARD_WORK = "gateway_sms_sos_forward";

    private GatewaySmsForwardScheduler() {
    }

    public static void enqueue(@NonNull Context context) {
        Constraints constraints = new Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build();

        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(GatewaySmsForwardWorker.class)
            .setConstraints(constraints)
            .build();

        WorkManager.getInstance(context.getApplicationContext())
            .enqueueUniqueWork(GATEWAY_SMS_FORWARD_WORK, ExistingWorkPolicy.KEEP, request);
    }
}
