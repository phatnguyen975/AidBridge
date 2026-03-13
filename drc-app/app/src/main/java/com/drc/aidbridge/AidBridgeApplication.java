package com.drc.aidbridge;

import android.app.Application;
import dagger.hilt.android.HiltAndroidApp;

/**
 * AidBridgeApplication — the entry point for Hilt DI.
 * Annotated with @HiltAndroidApp to trigger Hilt's code generation
 * and set up the application-level component used throughout the app.
 */
@HiltAndroidApp
public class AidBridgeApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
    }
}
