package com.drc.aidbridge.domain.usecase.user;

import androidx.lifecycle.LiveData;

import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.domain.model.User;
import com.drc.aidbridge.domain.repository.UserRepository;

import javax.inject.Inject;

public class GetCachedUserUseCase {

    private final UserRepository userRepository;

    @Inject
    public GetCachedUserUseCase(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public LiveData<NetworkResultWrapper<User>> execute() {
        return userRepository.getCachedUser();
    }
}
