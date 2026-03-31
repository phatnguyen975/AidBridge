package com.drc.aidbridge.modules.user;

import java.util.Optional;
import java.util.UUID;

public interface UserFacade {

    UserDTO getUserById(UUID userId);

    Optional<UserDTO> findUserByEmail(String email);

    boolean existsByEmail(String email);
}
