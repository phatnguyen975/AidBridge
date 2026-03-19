package com.drc.aidbridge.data.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.drc.aidbridge.data.remote.api.AuthApiService;
import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.data.remote.dto.response.AuthResponse;
import com.drc.aidbridge.data.remote.dto.response.UserDto;
import com.drc.aidbridge.data.remote.dto.request.ForgotPasswordRequest;
import com.drc.aidbridge.data.remote.dto.request.LoginRequest;
import com.drc.aidbridge.data.remote.dto.request.OtpVerifyRequest;
import com.drc.aidbridge.data.remote.dto.request.RegisterRequest;
import com.drc.aidbridge.data.remote.dto.request.ResetPasswordRequest;
import com.drc.aidbridge.domain.model.User;
import com.drc.aidbridge.domain.enums.UserRole;
import com.drc.aidbridge.domain.repository.AuthRepository;
import com.drc.aidbridge.utils.TokenManager;
import com.drc.aidbridge.data.mapper.UserMapper;

import org.json.JSONObject;

import javax.inject.Inject;
import javax.inject.Singleton;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * AuthRepositoryImpl — concrete implementation of AuthRepository that interacts with the AuthApiService.
 * It handles API calls, response parsing, error handling, and token management.
 * ViewModels depend on the AuthRepository interface, so they remain decoupled from the actual data-fetching logic.
 */
@Singleton
public class AuthRepositoryImpl extends BaseRepository implements AuthRepository {

    private final AuthApiService authApiService;
    private final TokenManager tokenManager;
    private final UserMapper userMapper;

    @Inject
    public AuthRepositoryImpl(AuthApiService authApiService,
                              TokenManager tokenManager,
                              UserMapper userMapper) {
        this.authApiService = authApiService;
        this.tokenManager = tokenManager;
        this.userMapper = userMapper;
    }

    @Override
    public LiveData<NetworkResultWrapper<User>> login(LoginRequest request) {
        MutableLiveData<NetworkResultWrapper<User>> result = new MutableLiveData<>();
        result.postValue(NetworkResultWrapper.loading());

        // TODO: Re-enable actual API call once backend is ready.
        /*
        authApiService.login(request).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                if (!response.isSuccessful()) {
                    result.postValue(NetworkResultWrapper.error(extractHttpError(response), response.code()));
                    return;
                }

                AuthResponse body = response.body();
                if (body == null || body.getUser() == null) {
                    result.postValue(NetworkResultWrapper.error("Phản hồi đăng nhập không hợp lệ."));
                    return;
                }

                persistAuthData(body);
                User user = userMapper.mapToDomain(body.getUser());
                tokenManager.saveUserInfo(user.getId(), user.getName(), user.getEmail(), user.getRole().name());
                result.postValue(NetworkResultWrapper.success(user));
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                result.postValue(NetworkResultWrapper.error("Không thể kết nối máy chủ: " + safeMessage(t)));
            }
        });
        */

        User mockUser = new User(
                "mock-user-id",
                "Mock Victim",
                request.getEmail(),
                "0000000000",
                UserRole.VICTIM,
                null
        );
        tokenManager.saveUserInfo(
                mockUser.getId(),
                mockUser.getName(),
                mockUser.getEmail(),
                mockUser.getRole().name()
        );
        result.postValue(NetworkResultWrapper.success(mockUser));

        return result;
    }

    @Override
    public LiveData<NetworkResultWrapper<String>> register(RegisterRequest request) {
        MutableLiveData<NetworkResultWrapper<String>> result = new MutableLiveData<>();
        result.postValue(NetworkResultWrapper.loading());

        // TODO: Re-enable actual API call once backend is ready.
        /*
        authApiService.register(request).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                if (!response.isSuccessful()) {
                    result.postValue(NetworkResultWrapper.error(extractHttpError(response), response.code()));
                    return;
                }

                AuthResponse body = response.body();
                if (body != null) {
                    persistAuthData(body);
                    if (body.getUser() != null) {
                        User user = userMapper.mapToDomain(body.getUser());
                        tokenManager.saveUserInfo(user.getId(), user.getName(), user.getEmail(), user.getRole().name());
                    }
                }

                result.postValue(NetworkResultWrapper.success(request.getEmail()));
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                result.postValue(NetworkResultWrapper.error("Đăng ký thất bại: " + safeMessage(t)));
            }
        });
        */

        result.postValue(NetworkResultWrapper.success(request.getEmail()));

        return result;
    }

    @Override
    public LiveData<NetworkResultWrapper<String>> verifyOtp(OtpVerifyRequest request) {
        MutableLiveData<NetworkResultWrapper<String>> result = new MutableLiveData<>();
        result.postValue(NetworkResultWrapper.loading());

        // TODO: Re-enable actual API call once backend is ready.
        /*
        authApiService.verifyOtp(request).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                if (!response.isSuccessful()) {
                    result.postValue(NetworkResultWrapper.error(extractHttpError(response), response.code()));
                    return;
                }

                AuthResponse body = response.body();
                if (body != null) {
                    persistAuthData(body);
                    if (body.getUser() != null) {
                        User user = userMapper.mapToDomain(body.getUser());
                        tokenManager.saveUserInfo(user.getId(), user.getName(), user.getEmail(), user.getRole().name());
                    }
                }

                result.postValue(NetworkResultWrapper.success(request.getEmail()));
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                result.postValue(NetworkResultWrapper.error("Xác thực OTP thất bại: " + safeMessage(t)));
            }
        });
        */

        result.postValue(NetworkResultWrapper.success(request.getEmail()));

        return result;
    }

    @Override
    public LiveData<NetworkResultWrapper<Boolean>> resendOtp(ForgotPasswordRequest request) {
        MutableLiveData<NetworkResultWrapper<Boolean>> result = new MutableLiveData<>();
        result.postValue(NetworkResultWrapper.loading());

        // TODO: Re-enable actual API call once backend is ready.
        /*
        authApiService.resendOtp(request).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (!response.isSuccessful()) {
                    result.postValue(NetworkResultWrapper.error(extractHttpError(response), response.code()));
                    return;
                }
                result.postValue(NetworkResultWrapper.success(Boolean.TRUE));
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                result.postValue(NetworkResultWrapper.error("Gửi lại OTP thất bại: " + safeMessage(t)));
            }
        });
        */

        result.postValue(NetworkResultWrapper.success(Boolean.TRUE));

        return result;
    }

    @Override
    public LiveData<NetworkResultWrapper<String>> forgotPassword(ForgotPasswordRequest request) {
        MutableLiveData<NetworkResultWrapper<String>> result = new MutableLiveData<>();
        result.postValue(NetworkResultWrapper.loading());

        // TODO: Re-enable actual API call once backend is ready.
        /*
        authApiService.forgotPassword(request).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (!response.isSuccessful()) {
                    result.postValue(NetworkResultWrapper.error(extractHttpError(response), response.code()));
                    return;
                }
                result.postValue(NetworkResultWrapper.success(request.getEmail()));
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                result.postValue(NetworkResultWrapper.error("Không gửi được OTP: " + safeMessage(t)));
            }
        });
        */

        result.postValue(NetworkResultWrapper.success(request.getEmail()));

        return result;
    }

    @Override
    public LiveData<NetworkResultWrapper<String>> verifyResetOtp(OtpVerifyRequest request) {
        MutableLiveData<NetworkResultWrapper<String>> result = new MutableLiveData<>();
        result.postValue(NetworkResultWrapper.loading());

        // TODO: Re-enable actual API call once backend is ready.
        /*
        authApiService.verifyResetOtp(request).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (!response.isSuccessful()) {
                    result.postValue(NetworkResultWrapper.error(extractHttpError(response), response.code()));
                    return;
                }
                result.postValue(NetworkResultWrapper.success(request.getEmail()));
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                result.postValue(NetworkResultWrapper.error("Xác thực OTP thất bại: " + safeMessage(t)));
            }
        });
        */

        result.postValue(NetworkResultWrapper.success(request.getEmail()));

        return result;
    }

    @Override
    public LiveData<NetworkResultWrapper<String>> resetPassword(ResetPasswordRequest request) {
        MutableLiveData<NetworkResultWrapper<String>> result = new MutableLiveData<>();
        result.postValue(NetworkResultWrapper.loading());

        // TODO: Re-enable actual API call once backend is ready.
        /*
        authApiService.resetPassword(request).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (!response.isSuccessful()) {
                    result.postValue(NetworkResultWrapper.error(extractHttpError(response), response.code()));
                    return;
                }
                result.postValue(NetworkResultWrapper.success("Đổi mật khẩu thành công"));
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                result.postValue(NetworkResultWrapper.error("Đổi mật khẩu thất bại: " + safeMessage(t)));
            }
        });
        */

        result.postValue(NetworkResultWrapper.success("Đổi mật khẩu thành công"));

        return result;
    }

    private void persistAuthData(AuthResponse body) {
        if (body.getAccessToken() != null && body.getRefreshToken() != null) {
            tokenManager.saveTokens(body.getAccessToken(), body.getRefreshToken());
        }
    }
}
