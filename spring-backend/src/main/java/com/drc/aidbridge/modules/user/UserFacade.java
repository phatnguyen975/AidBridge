package com.drc.aidbridge.modules.user;

import java.util.Optional;
import java.util.UUID;
import java.util.List;
import com.drc.aidbridge.modules.shared.enums.UserRole;

public interface UserFacade {

    UserDTO getUserById(UUID userId);

    Optional<UserDTO> findUserByEmail(String email);

    boolean existsByEmail(String email);

    List<UserDTO> findUsersByRole(UserRole role);

    UserDTO createUser(CreateUserRequest request);
}
