package com.drc.aidbridge.di;

import dagger.Binds;
import dagger.Module;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;
import javax.inject.Singleton;

import com.drc.aidbridge.data.repository.AuthRepositoryImpl;
import com.drc.aidbridge.domain.repository.AuthRepository;

/**
 * RepositoryModule — binds Repository interfaces to their concrete implementations.
 *
 * Uses @Binds (more efficient than @Provides) since we're just mapping an interface
 * to an already @Inject-annotated implementation class.
 */
@Module
@InstallIn(SingletonComponent.class)
public abstract class RepositoryModule {

    /**
     * Binds AuthRepository interface to AuthRepositoryImpl.
     * When a UseCase or ViewModel requests AuthRepository via @Inject,
     * Hilt will provide the AuthRepositoryImpl singleton.
     */
    @Binds
    @Singleton
    public abstract AuthRepository bindAuthRepository(AuthRepositoryImpl impl);
}
