package com.drc.aidbridge.domain.usecase.user;

import androidx.lifecycle.LiveData;

import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.domain.model.User;
import com.drc.aidbridge.domain.repository.UserRepository;

import javax.inject.Inject;

public class GetCurrentUserUseCase {

    private final UserRepository userRepository;

    @Inject
    public GetCurrentUserUseCase(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public LiveData<NetworkResultWrapper<User>> execute() {
        return userRepository.getCurrentUser();
    }
}
