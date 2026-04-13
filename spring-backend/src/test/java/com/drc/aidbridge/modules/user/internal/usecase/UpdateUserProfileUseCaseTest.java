package com.drc.aidbridge.modules.user.internal.usecase;

import com.drc.aidbridge.modules.shared.exception.DuplicateResourceException;
import com.drc.aidbridge.modules.user.internal.entity.User;
import com.drc.aidbridge.modules.user.internal.mapper.UserMapper;
import com.drc.aidbridge.modules.user.internal.repository.UserJpaRepository;
import com.drc.aidbridge.modules.user.internal.web.dto.UpdateUserProfileRequest;
import com.drc.aidbridge.modules.user.internal.web.dto.UserResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UpdateUserProfileUseCaseTest {

    private UserJpaRepository userRepository;
    private UserMapper userMapper;
    private UpdateUserProfileUseCase useCase;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserJpaRepository.class);
        userMapper = mock(UserMapper.class);
        useCase = new UpdateUserProfileUseCase(userRepository, userMapper);
    }

    @Test
    void execute_ShouldUpdateProfile_WhenRequestIsValid() {
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .id(userId)
                .fullName("Old Name")
                .phoneNumber("0900000000")
                .build();

        UpdateUserProfileRequest request = UpdateUserProfileRequest.builder()
                .name("New Name")
                .phone("0912345678")
                .address("Somewhere")
                .build();

        UserResponse expectedResponse = UserResponse.builder()
                .id(userId.toString())
                .fullName("New Name")
                .phoneNumber("0912345678")
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.findByPhoneNumber("0912345678")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userMapper.toResponse(any(User.class))).thenReturn(expectedResponse);

        UserResponse response = useCase.execute(userId, request);

        assertEquals("New Name", response.getFullName());
        assertEquals("0912345678", response.getPhoneNumber());
        verify(userRepository).save(user);
    }

    @Test
    void execute_ShouldThrowDuplicateResource_WhenPhoneBelongsToAnotherUser() {
        UUID userId = UUID.randomUUID();
        UUID anotherUserId = UUID.randomUUID();

        User user = User.builder()
                .id(userId)
                .fullName("Current User")
                .phoneNumber("0900000000")
                .build();

        User anotherUser = User.builder()
                .id(anotherUserId)
                .fullName("Another User")
                .phoneNumber("0912345678")
                .build();

        UpdateUserProfileRequest request = UpdateUserProfileRequest.builder()
                .name("Current User")
                .phone("0912345678")
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.findByPhoneNumber("0912345678")).thenReturn(Optional.of(anotherUser));

        assertThrows(DuplicateResourceException.class, () -> useCase.execute(userId, request));
    }
}
