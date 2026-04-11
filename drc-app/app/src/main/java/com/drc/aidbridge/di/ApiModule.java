package com.drc.aidbridge.di;

import com.drc.aidbridge.data.remote.api.AuthApiService;
import com.drc.aidbridge.data.remote.api.UserApiService;
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

/**
 * ApiModule — provides all API service interfaces.
 * 
 * Depends on Retrofit provided by NetworkModule to create implementations of
 * API interfaces.
 */
@Module
@InstallIn(SingletonComponent.class)
public class ApiModule {

    /** Provides the Auth API service (public endpoints: login, register, OTP). */
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
    public SosApiService provideSosApiService(Retrofit retrofit) {
        return retrofit.create(SosApiService.class);
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
}
