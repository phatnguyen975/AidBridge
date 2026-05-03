package com.drc.aidbridge.worker;

import com.drc.aidbridge.data.local.dao.GatewayPendingSmsDao;
import com.drc.aidbridge.data.local.dao.LocalQuickSosDao;
import com.drc.aidbridge.data.remote.api.gateway.SmsIngestApiService;
import com.drc.aidbridge.data.remote.api.victim.SosApiService;

import dagger.hilt.EntryPoint;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;

@EntryPoint
@InstallIn(SingletonComponent.class)
public interface SmsFallbackWorkerEntryPoint {
    LocalQuickSosDao localQuickSosDao();
    GatewayPendingSmsDao gatewayPendingSmsDao();
    SosApiService sosApiService();
    SmsIngestApiService smsIngestApiService();
}
