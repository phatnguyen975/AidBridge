package com.drc.aidbridge.di;

import com.drc.aidbridge.data.remote.interceptor.AuthInterceptor;
import com.drc.aidbridge.data.remote.interceptor.TokenRefreshInterceptor;
import com.drc.aidbridge.utils.Constants;

import java.util.concurrent.TimeUnit;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * NetworkModule — provides all networking-related singletons.
 * Contains OkHttpClient (with auth & token refresh interceptors) and Retrofit.
 */
@Module
@InstallIn(SingletonComponent.class)
public class NetworkModule {

    /**
     * Provides OkHttpClient with:
     * - AuthInterceptor (attaches JWT Authorization header to authenticated requests)
     * - TokenRefreshInterceptor (handles 401 by refreshing the token and retrying)
     * - HttpLoggingInterceptor (for debug logging)
     */
    @Provides
    @Singleton
    public OkHttpClient provideOkHttpClient(
            AuthInterceptor authInterceptor,
            TokenRefreshInterceptor tokenRefreshInterceptor) {

        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY); // Change to Level.NONE for release

        return new OkHttpClient.Builder()
                .connectTimeout(Constants.CONNECT_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(Constants.READ_TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(Constants.WRITE_TIMEOUT, TimeUnit.SECONDS)
                .addInterceptor(authInterceptor)         // Attach Bearer token to every request
                .addInterceptor(tokenRefreshInterceptor) // Auto-refresh on 401 responses
                .addInterceptor(loggingInterceptor)
                .build();
    }

    /**
     * Provides the main Retrofit instance configured with:
     * - BASE_URL from Constants
     * - Gson converter for automatic JSON ↔ Java object mapping
     */
    @Provides
    @Singleton
    public Retrofit provideRetrofit(OkHttpClient okHttpClient) {
        return new Retrofit.Builder()
                .baseUrl(Constants.BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }
}
