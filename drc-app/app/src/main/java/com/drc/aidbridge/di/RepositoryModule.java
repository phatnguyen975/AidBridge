package com.drc.aidbridge.di;

import com.drc.aidbridge.data.repository.AuthRepositoryImpl;
import com.drc.aidbridge.data.repository.MissionRepositoryImpl;
import com.drc.aidbridge.data.repository.RoutingRepositoryImpl;
import com.drc.aidbridge.data.repository.UserRepositoryImpl;
import com.drc.aidbridge.data.repository.victim.VictimHistoryRepositoryImpl;
import com.drc.aidbridge.data.repository.victim.VictimSosRepositoryImpl;
import com.drc.aidbridge.data.repository.victim.VictimSupplyRepositoryImpl;
import com.drc.aidbridge.data.repository.volunteer.VolunteerRepositoryImpl;
import com.drc.aidbridge.domain.repository.AuthRepository;
import com.drc.aidbridge.domain.repository.MissionRepository;
import com.drc.aidbridge.domain.repository.RoutingRepository;
import com.drc.aidbridge.domain.repository.UserRepository;
import com.drc.aidbridge.domain.repository.volunteer.VolunteerRepository;
import com.drc.aidbridge.domain.repository.victim.VictimHistoryRepository;
import com.drc.aidbridge.domain.repository.victim.VictimSosRepository;
import com.drc.aidbridge.domain.repository.victim.VictimSupplyRepository;

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
    public abstract MissionRepository bindMissionRepository(MissionRepositoryImpl impl);

    @Binds
    @Singleton
    public abstract RoutingRepository bindRoutingRepository(RoutingRepositoryImpl impl);

    @Binds
    @Singleton
    public abstract VolunteerRepository bindVolunteerRepository(VolunteerRepositoryImpl impl);

    @Binds
    @Singleton
    public abstract VictimSosRepository bindVictimSosRepository(VictimSosRepositoryImpl impl);

    @Binds
    @Singleton
    public abstract VictimSupplyRepository bindVictimSupplyRepository(VictimSupplyRepositoryImpl impl);

    @Binds
    @Singleton
    public abstract VictimHistoryRepository bindVictimHistoryRepository(VictimHistoryRepositoryImpl impl);
}
