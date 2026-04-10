package com.drc.aidbridge.di;

import com.drc.aidbridge.data.repository.AuthRepositoryImpl;
import com.drc.aidbridge.data.repository.SosRepositoryImpl;
import com.drc.aidbridge.data.repository.SupplyRepositoryImpl;
import com.drc.aidbridge.data.repository.UserRepositoryImpl;
import com.drc.aidbridge.data.repository.VictimHistoryRepositoryImpl;
import com.drc.aidbridge.data.repository.volunteer.VolunteerRepositoryImpl;
import com.drc.aidbridge.domain.repository.AuthRepository;
import com.drc.aidbridge.domain.repository.SosRepository;
import com.drc.aidbridge.domain.repository.SupplyRepository;
import com.drc.aidbridge.domain.repository.UserRepository;
import com.drc.aidbridge.domain.repository.VictimHistoryRepository;
import com.drc.aidbridge.domain.repository.volunteer.VolunteerRepository;

import dagger.Binds;
import dagger.Module;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;
import javax.inject.Singleton;

/**
 * RepositoryModule — binds Repository interfaces to their concrete
 * implementations.
 *
 * Uses @Binds (more efficient than @Provides) since we're just mapping an
 * interface
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

    @Binds
    @Singleton
    public abstract UserRepository bindUserRepository(UserRepositoryImpl impl);

    @Binds
    @Singleton
    public abstract VolunteerRepository bindVolunteerRepository(VolunteerRepositoryImpl impl);

    @Binds
    @Singleton
    public abstract SosRepository bindSosRepository(SosRepositoryImpl impl);

    @Binds
    @Singleton
    public abstract SupplyRepository bindSupplyRepository(SupplyRepositoryImpl impl);

    @Binds
    @Singleton
    public abstract VictimHistoryRepository bindVictimHistoryRepository(VictimHistoryRepositoryImpl impl);
}
