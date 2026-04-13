package com.drc.aidbridge.di;

import com.drc.aidbridge.data.remote.api.AuthApiService;
import com.drc.aidbridge.data.remote.api.MissionApiService;
import com.drc.aidbridge.data.remote.api.UserApiService;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;
import retrofit2.Retrofit;

/**
 * ApiModule — provides all API service interfaces.
 * 
 * Depends on Retrofit provided by NetworkModule to create implementations of API interfaces.
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
    public MissionApiService provideMissionApiService(Retrofit retrofit) {
        return retrofit.create(MissionApiService.class);
    }
}
