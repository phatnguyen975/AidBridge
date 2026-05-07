package com.drc.aidbridge.di;

import com.drc.aidbridge.data.repository.AuthRepositoryImpl;
import com.drc.aidbridge.data.repository.MissionRepositoryImpl;
import com.drc.aidbridge.data.repository.RoutingRepositoryImpl;
import com.drc.aidbridge.data.repository.UserRepositoryImpl;
import com.drc.aidbridge.data.repository.admin.AdminDashboardRepositoryImpl;
import com.drc.aidbridge.data.repository.admin.AdminStaffRepositoryImpl;
import com.drc.aidbridge.data.repository.staff.StaffInventoryRepositoryImpl;
import com.drc.aidbridge.data.repository.staff.StaffTasksRepositoryImpl;
import com.drc.aidbridge.data.repository.sponsor.SponsorDonationRepositoryImpl;
import com.drc.aidbridge.data.repository.victim.VictimHistoryRepositoryImpl;
import com.drc.aidbridge.data.repository.victim.VictimSosRepositoryImpl;
import com.drc.aidbridge.data.repository.victim.VictimSupplyRepositoryImpl;
import com.drc.aidbridge.data.repository.volunteer.VolunteerRepositoryImpl;
import com.drc.aidbridge.domain.repository.AuthRepository;
import com.drc.aidbridge.domain.repository.MissionRepository;
import com.drc.aidbridge.domain.repository.RoutingRepository;
import com.drc.aidbridge.domain.repository.UserRepository;
import com.drc.aidbridge.domain.repository.admin.AdminDashboardRepository;
import com.drc.aidbridge.domain.repository.admin.AdminStaffRepository;
import com.drc.aidbridge.domain.repository.staff.StaffInventoryRepository;
import com.drc.aidbridge.domain.repository.staff.StaffTasksRepository;
import com.drc.aidbridge.domain.repository.sponsor.SponsorDonationRepository;
import com.drc.aidbridge.domain.repository.volunteer.VolunteerRepository;
import com.drc.aidbridge.domain.repository.victim.VictimHistoryRepository;
import com.drc.aidbridge.domain.repository.victim.VictimSosRepository;
import com.drc.aidbridge.domain.repository.victim.VictimSupplyRepository;
import com.drc.aidbridge.domain.repository.HubRepository;
import dagger.Binds;
import dagger.Module;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;
import javax.inject.Singleton;

@Module
@InstallIn(SingletonComponent.class)
public abstract class RepositoryModule {

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

    // Mapping for Default HubRepository
    @Binds
    @Singleton
    public abstract com.drc.aidbridge.domain.repository.HubRepository bindDefaultHubRepository(
            com.drc.aidbridge.data.repository.HubRepositoryImpl impl);

    // Mapping for Admin HubRepository
    @Binds
    @Singleton
    public abstract com.drc.aidbridge.domain.repository.admin.HubRepository bindAdminHubRepository(
            com.drc.aidbridge.data.repository.admin.HubRepositoryImpl impl);

    @Binds
    @Singleton
    public abstract AdminDashboardRepository bindAdminDashboardRepository(AdminDashboardRepositoryImpl impl);

    @Binds
    @Singleton
    public abstract AdminStaffRepository bindAdminStaffRepository(AdminStaffRepositoryImpl impl);

    @Binds
    @Singleton
    public abstract StaffInventoryRepository bindStaffInventoryRepository(StaffInventoryRepositoryImpl impl);

        @Binds
        @Singleton
        public abstract StaffTasksRepository bindStaffTasksRepository(StaffTasksRepositoryImpl impl);

    @Binds
    @Singleton
    public abstract SponsorDonationRepository bindSponsorDonationRepository(SponsorDonationRepositoryImpl impl);
}
