package com.drc.aidbridge.data.mapper;

import com.drc.aidbridge.data.remote.dto.response.UserDto;
import com.drc.aidbridge.domain.model.User;
import com.drc.aidbridge.domain.enums.UserRole;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class UserMapper implements BaseMapper<UserDto, User> {

    @Inject
    public UserMapper() {
    }

    @Override
    public User mapToDomain(UserDto dto) {
        if (dto == null) {
            return null;
        }

        return new User(
                dto.getId(),
                dto.getName(),
                dto.getEmail(),
                dto.getPhone(),
                UserRole.fromStringSafe(dto.getRole()),
                dto.getAvatarUrl(),
                dto.isVerified()
        );
    }

    @Override
    public UserDto mapToDto(User domainModel) {
        if (domainModel == null) {
            return null;
        }

        return new UserDto(
                domainModel.getId(),
                domainModel.getName(),
                domainModel.getEmail(),
                domainModel.getPhone(),
                domainModel.getRole().name(),
                domainModel.getAvatarUrl(),
                domainModel.isVerified()
        );
    }
}
