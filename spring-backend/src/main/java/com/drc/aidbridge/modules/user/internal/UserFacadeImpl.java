package com.drc.aidbridge.modules.user.internal;

import com.drc.aidbridge.exception.ResourceNotFoundException;
import com.drc.aidbridge.modules.user.UserDTO;
import com.drc.aidbridge.modules.user.UserFacade;
import com.drc.aidbridge.modules.user.internal.mapper.UserMapper;
import com.drc.aidbridge.modules.user.internal.repository.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserFacadeImpl implements UserFacade {

    private final UserJpaRepository userRepository;
    private final UserMapper userMapper;

    @Override
    public UserDTO getUserById(UUID userId) {
        return userRepository.findById(userId)
                .map(userMapper::toDTO)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
    }

    @Override
    public Optional<UserDTO> findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .map(userMapper::toDTO);
    }

    @Override
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }
}
