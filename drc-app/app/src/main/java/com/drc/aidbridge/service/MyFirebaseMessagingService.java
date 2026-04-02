package com.drc.aidbridge.service;

import android.provider.Settings;

import androidx.annotation.NonNull;

import com.drc.aidbridge.domain.repository.AuthRepository;
import com.drc.aidbridge.utils.TokenManager;
import com.google.firebase.messaging.FirebaseMessagingService;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private final ExecutorService backgroundExecutor = Executors.newSingleThreadExecutor();

    @Inject
    TokenManager tokenManager;

    @Inject
    AuthRepository authRepository;

    @Override
    public void onNewToken(@NonNull String newToken) {
        super.onNewToken(newToken);

        String sanitizedToken = sanitize(newToken);
        if (sanitizedToken == null) {
            return;
        }

        tokenManager.saveFcmToken(sanitizedToken);

        if (!tokenManager.hasActiveSession()) {
            return;
        }

        String deviceId = sanitize(Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID));
        if (deviceId == null) {
            return;
        }

        backgroundExecutor.execute(() -> authRepository.updateFcmToken(deviceId, sanitizedToken));
    }

    @Override
    public void onDestroy() {
        backgroundExecutor.shutdown();
        super.onDestroy();
    }

    private String sanitize(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
