package com.drc.aidbridge.domain.usecase.user;

import androidx.lifecycle.LiveData;

import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.domain.repository.UserRepository;

import javax.inject.Inject;

import okhttp3.MultipartBody;

public class UploadAvatarUseCase {

    private final UserRepository userRepository;

    @Inject
    public UploadAvatarUseCase(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public LiveData<NetworkResultWrapper<String>> execute(MultipartBody.Part avatarPart) {
        return userRepository.uploadAvatar(avatarPart);
    }
}
