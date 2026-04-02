package com.drc.aidbridge.domain.usecase.auth;

import androidx.lifecycle.LiveData;

import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.domain.repository.AuthRepository;
import com.drc.aidbridge.utils.TokenManager;

import javax.inject.Inject;

/**
 * LogoutUseCase triggers backend token revocation and local session cleanup.
 */
public class LogoutUseCase {

    private final AuthRepository authRepository;
    private final TokenManager tokenManager;

    @Inject
    public LogoutUseCase(AuthRepository authRepository, TokenManager tokenManager) {
        this.authRepository = authRepository;
        this.tokenManager = tokenManager;
    }

    public LiveData<NetworkResultWrapper<Boolean>> execute() {
        String refreshToken = tokenManager.getRefreshToken();
        return authRepository.logout(refreshToken);
    }
}
