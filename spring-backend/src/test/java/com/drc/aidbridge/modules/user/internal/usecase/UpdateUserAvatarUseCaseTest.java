package com.drc.aidbridge.modules.user.internal.usecase;

import com.drc.aidbridge.modules.attachment.AttachmentDTO;
import com.drc.aidbridge.modules.attachment.AttachmentFacade;
import com.drc.aidbridge.modules.shared.exception.BadRequestException;
import com.drc.aidbridge.modules.user.internal.entity.User;
import com.drc.aidbridge.modules.user.internal.repository.UserJpaRepository;
import com.drc.aidbridge.modules.user.internal.web.dto.UserAvatarUploadResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UpdateUserAvatarUseCaseTest {

    private UserJpaRepository userRepository;
    private AttachmentFacade attachmentFacade;
    private UpdateUserAvatarUseCase useCase;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserJpaRepository.class);
        attachmentFacade = mock(AttachmentFacade.class);
        useCase = new UpdateUserAvatarUseCase(userRepository, attachmentFacade);
    }

    @Test
    void execute_ShouldThrowBadRequest_WhenAvatarFileIsEmpty() {
        MultipartFile avatarFile = mock(MultipartFile.class);
        when(avatarFile.isEmpty()).thenReturn(true);

        assertThrows(BadRequestException.class, () -> useCase.execute(UUID.randomUUID(), avatarFile));
    }

    @Test
    void execute_ShouldUploadAndUpdateAvatar_WhenFileIsValid() {
        UUID userId = UUID.randomUUID();
        MultipartFile avatarFile = mock(MultipartFile.class);

        User user = User.builder()
                .id(userId)
                .fullName("User")
                .email("user@test.com")
                .build();

        AttachmentDTO attachmentDTO = AttachmentDTO.builder()
                .url("https://cdn.example.com/avatar.jpg")
                .build();

        when(avatarFile.isEmpty()).thenReturn(false);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(attachmentFacade.upload(eq(userId), eq(avatarFile), eq("USER_AVATAR"), eq(userId)))
                .thenReturn(attachmentDTO);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserAvatarUploadResponse response = useCase.execute(userId, avatarFile);

        assertEquals("https://cdn.example.com/avatar.jpg", response.getAvatarUrl());
        assertEquals("https://cdn.example.com/avatar.jpg", user.getAvatarUrl());
        verify(userRepository).save(user);
    }
}
