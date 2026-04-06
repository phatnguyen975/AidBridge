package com.drc.aidbridge.data.mapper.volunteer;

import androidx.annotation.Nullable;

import com.drc.aidbridge.data.mapper.BaseMapper;
import com.drc.aidbridge.data.remote.dto.response.volunteer.VolunteerProfileDataDto;
import com.drc.aidbridge.data.remote.dto.response.volunteer.VolunteerUserDto;
import com.drc.aidbridge.domain.model.volunteer.VolunteerDashboardInfo;
import com.drc.aidbridge.domain.model.volunteer.VolunteerPersonalInfo;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class VolunteerInfoMapper implements BaseMapper<VolunteerProfileDataDto, VolunteerDashboardInfo> {

    @Inject
    public VolunteerInfoMapper() {
    }

    @Override
    public VolunteerDashboardInfo mapToDomain(@Nullable VolunteerProfileDataDto dto) {
        return mapToDashboardInfoDomain(dto);
    }

    @Override
    public VolunteerProfileDataDto mapToDto(VolunteerDashboardInfo domainModel) {
        return null;
    }

    public VolunteerDashboardInfo mapToDashboardInfoDomain(@Nullable VolunteerProfileDataDto dto) {
        if (dto == null) {
            return new VolunteerDashboardInfo("", false, 0);
        }

        VolunteerUserDto user = dto.getUser();
        String fullName = user != null && user.getName() != null
                ? user.getName().trim()
                : "";

        VolunteerProfileDataDto.ProfileDto profile = dto.getProfile();
        boolean isOnline = profile != null && profile.isOnline();
        int totalCompletedTasks = profile != null ? profile.getTotalTasksCompleted() : 0;

        return new VolunteerDashboardInfo(
                fullName,
                isOnline,
                Math.max(totalCompletedTasks, 0));
    }

    public VolunteerPersonalInfo mapToPersonalInfoDomain(@Nullable VolunteerProfileDataDto dto) {
        if (dto == null) {
            return new VolunteerPersonalInfo("", "", "");
        }

        VolunteerUserDto user = dto.getUser();
        String fullName = user != null && user.getName() != null
                ? user.getName().trim()
                : "";
        String phoneNumber = user != null && user.getPhoneNumber() != null
                ? user.getPhoneNumber().trim()
                : "";
        String email = user != null && user.getEmail() != null
                ? user.getEmail().trim()
                : "";

        return new VolunteerPersonalInfo(fullName, phoneNumber, email);
    }
}
