package com.drc.aidbridge.di;

import com.drc.aidbridge.data.remote.api.AuthApiService;
import com.drc.aidbridge.data.remote.api.MissionApiService;
import com.drc.aidbridge.data.remote.api.RoutingApiService;
import com.drc.aidbridge.data.remote.api.UserApiService;
import com.drc.aidbridge.data.remote.api.admin.AdminDashboardApiService;
import com.drc.aidbridge.data.remote.api.admin.AdminStaffApiService;
import com.drc.aidbridge.data.remote.api.gateway.SmsIngestApiService;
import com.drc.aidbridge.data.remote.api.admin.HubApiService;
import com.drc.aidbridge.data.remote.api.staff.StaffApiService;
import com.drc.aidbridge.data.remote.api.staff.StaffInventoryApiService;
import com.drc.aidbridge.data.remote.api.sponsor.SponsorDonationApiService;
import com.drc.aidbridge.data.remote.api.volunteer.VolunteerApiService;
import com.drc.aidbridge.data.remote.api.victim.HistoryApiService;
import com.drc.aidbridge.data.remote.api.victim.SosApiService;
import com.drc.aidbridge.data.remote.api.victim.SupplyApiService;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;
import retrofit2.Retrofit;

@Module
@InstallIn(SingletonComponent.class)
public class ApiModule {

    @Provides
    @Singleton
    public AuthApiService provideAuthApiService(Retrofit retrofit) {
        return retrofit.create(AuthApiService.class);
    }

    @Provides
    @Singleton
    public UserApiService provideUserApiService(Retrofit retrofit) {
        return retrofit.create(UserApiService.class);
    }

    @Provides
    @Singleton
    public VolunteerApiService provideVolunteerApiService(Retrofit retrofit) {
        return retrofit.create(VolunteerApiService.class);
    }

    @Provides
    @Singleton
    public MissionApiService provideMissionApiService(Retrofit retrofit) {
        return retrofit.create(MissionApiService.class);
    }

    @Provides
    @Singleton
    public SosApiService provideSosApiService(Retrofit retrofit) {
        return retrofit.create(SosApiService.class);
    }

    @Provides
    @Singleton
    public SmsIngestApiService provideSmsIngestApiService(Retrofit retrofit) {
        return retrofit.create(SmsIngestApiService.class);
    }

    @Provides
    @Singleton
    public SupplyApiService provideSupplyApiService(Retrofit retrofit) {
        return retrofit.create(SupplyApiService.class);
    }

    @Provides
    @Singleton
    public HistoryApiService provideHistoryApiService(Retrofit retrofit) {
        return retrofit.create(HistoryApiService.class);
    }

    // Admin HubApiService
    @Provides
    @Singleton
    public HubApiService provideAdminHubApiService(Retrofit retrofit) {
        return retrofit.create(HubApiService.class);
    }

    @Provides
    @Singleton
    public AdminDashboardApiService provideAdminDashboardApiService(Retrofit retrofit) {
        return retrofit.create(AdminDashboardApiService.class);
    }

    @Provides
    @Singleton
    public AdminStaffApiService provideAdminStaffApiService(Retrofit retrofit) {
        return retrofit.create(AdminStaffApiService.class);
    }

    @Provides
    @Singleton
    public StaffInventoryApiService provideStaffInventoryApiService(Retrofit retrofit) {
        return retrofit.create(StaffInventoryApiService.class);
    }

    @Provides
    @Singleton
    public StaffApiService provideStaffApiService(Retrofit retrofit) {
        return retrofit.create(StaffApiService.class);
    }

    // Default HubApiService
    @Provides
    @Singleton
    public com.drc.aidbridge.data.remote.api.hub.HubApiService provideDefaultHubApiService(Retrofit retrofit) {
        return retrofit.create(com.drc.aidbridge.data.remote.api.hub.HubApiService.class);
    }

    @Provides
    @Singleton
    public RoutingApiService provideRoutingApiService(Retrofit retrofit) {
        return retrofit.create(RoutingApiService.class);
    }

    @Provides
    @Singleton
    public SponsorDonationApiService provideSponsorDonationApiService(Retrofit retrofit) {
        return retrofit.create(SponsorDonationApiService.class);
    }
}
