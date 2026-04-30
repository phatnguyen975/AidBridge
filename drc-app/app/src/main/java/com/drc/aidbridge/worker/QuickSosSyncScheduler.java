package com.drc.aidbridge.worker;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

public final class QuickSosSyncScheduler {

    private static final String QUICK_SOS_SYNC_WORK = "quick_sos_sms_fallback_sync";

    private QuickSosSyncScheduler() {
    }

    public static void enqueue(@NonNull Context context) {
        Constraints constraints = new Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build();

        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(QuickSosSyncWorker.class)
            .setConstraints(constraints)
            .build();

        WorkManager.getInstance(context.getApplicationContext())
            .enqueueUniqueWork(QUICK_SOS_SYNC_WORK, ExistingWorkPolicy.KEEP, request);
    }
}
