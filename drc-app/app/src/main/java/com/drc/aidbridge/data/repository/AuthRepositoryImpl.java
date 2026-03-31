package com.drc.aidbridge.data.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.drc.aidbridge.data.remote.api.AuthApiService;
import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.data.remote.dto.response.AuthResponse;
import com.drc.aidbridge.data.remote.dto.response.BaseResponse;
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
        authApiService.login(request).enqueue(new Callback<BaseResponse<AuthResponse>>() {
            @Override
            public void onResponse(Call<BaseResponse<AuthResponse>> call,
                                   Response<BaseResponse<AuthResponse>> response) {
                if (!response.isSuccessful()) {
                    result.postValue(NetworkResultWrapper.error(extractHttpError(response), response.code()));
                    return;
                }

                BaseResponse<AuthResponse> baseResponse = response.body();
                if (baseResponse == null) {
                    result.postValue(NetworkResultWrapper.error("Phản hồi đăng nhập không hợp lệ."));
                    return;
                }

                if (!baseResponse.isSuccess()) {
                    String apiMessage = baseResponse.getMessage();
                    result.postValue(NetworkResultWrapper.error(
                        apiMessage != null && !apiMessage.isEmpty()
                            ? apiMessage
                            : "Đăng nhập thất bại."
                    ));
                    return;
                }

                AuthResponse data = baseResponse.getData();
                if (data == null || data.getUser() == null) {
                    String apiMessage = baseResponse.getMessage();
                    result.postValue(NetworkResultWrapper.error(
                        apiMessage != null && !apiMessage.isEmpty()
                            ? apiMessage
                            : "Phản hồi đăng nhập không hợp lệ."
                    ));
                    return;
                }

                persistAuthData(data);
                User user = userMapper.mapToDomain(data.getUser());
                tokenManager.saveUserInfo(user.getId(), user.getName(), user.getEmail(), user.getRole().name());
                result.postValue(NetworkResultWrapper.success(user));
            }

            @Override
            public void onFailure(Call<BaseResponse<AuthResponse>> call, Throwable t) {
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
        authApiService.register(request).enqueue(new Callback<BaseResponse<AuthResponse>>() {
            @Override
            public void onResponse(Call<BaseResponse<AuthResponse>> call,
                                   Response<BaseResponse<AuthResponse>> response) {
                if (!response.isSuccessful()) {
                    result.postValue(NetworkResultWrapper.error(extractHttpError(response), response.code()));
                    return;
                }

                BaseResponse<AuthResponse> baseResponse = response.body();
                if (baseResponse == null) {
                    result.postValue(NetworkResultWrapper.error("Phản hồi đăng ký không hợp lệ."));
                    return;
                }

                if (!baseResponse.isSuccess()) {
                    String apiMessage = baseResponse.getMessage();
                    result.postValue(NetworkResultWrapper.error(
                        apiMessage != null && !apiMessage.isEmpty()
                            ? apiMessage
                            : "Đăng ký thất bại."
                    ));
                    return;
                }

                AuthResponse data = baseResponse.getData();
                if (data != null) {
                    persistAuthData(data);
                    if (data.getUser() != null) {
                        User user = userMapper.mapToDomain(data.getUser());
                        tokenManager.saveUserInfo(user.getId(), user.getName(), user.getEmail(), user.getRole().name());
                    }
                }

                result.postValue(NetworkResultWrapper.success(request.getEmail()));
            }

            @Override
            public void onFailure(Call<BaseResponse<AuthResponse>> call, Throwable t) {
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
        authApiService.verifyOtp(request).enqueue(new Callback<BaseResponse<AuthResponse>>() {
            @Override
            public void onResponse(Call<BaseResponse<AuthResponse>> call,
                                   Response<BaseResponse<AuthResponse>> response) {
                if (!response.isSuccessful()) {
                    result.postValue(NetworkResultWrapper.error(extractHttpError(response), response.code()));
                    return;
                }

                BaseResponse<AuthResponse> baseResponse = response.body();
                if (baseResponse == null) {
                    result.postValue(NetworkResultWrapper.error("Phản hồi xác thực OTP không hợp lệ."));
                    return;
                }

                if (!baseResponse.isSuccess()) {
                    String apiMessage = baseResponse.getMessage();
                    result.postValue(NetworkResultWrapper.error(
                        apiMessage != null && !apiMessage.isEmpty()
                            ? apiMessage
                            : "Xác thực OTP thất bại."
                    ));
                    return;
                }

                AuthResponse data = baseResponse.getData();
                if (data != null) {
                    persistAuthData(data);
                    if (data.getUser() != null) {
                        User user = userMapper.mapToDomain(data.getUser());
                        tokenManager.saveUserInfo(user.getId(), user.getName(), user.getEmail(), user.getRole().name());
                    }
                }

                result.postValue(NetworkResultWrapper.success(request.getEmail()));
            }

            @Override
            public void onFailure(Call<BaseResponse<AuthResponse>> call, Throwable t) {
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
        authApiService.resendOtp(request).enqueue(new Callback<BaseResponse<Void>>() {
            @Override
            public void onResponse(Call<BaseResponse<Void>> call, Response<BaseResponse<Void>> response) {
                if (!response.isSuccessful()) {
                    result.postValue(NetworkResultWrapper.error(extractHttpError(response), response.code()));
                    return;
                }

                BaseResponse<Void> baseResponse = response.body();
                if (baseResponse == null) {
                    result.postValue(NetworkResultWrapper.error("Phản hồi gửi lại OTP không hợp lệ."));
                    return;
                }

                if (!baseResponse.isSuccess()) {
                    String apiMessage = baseResponse.getMessage();
                    result.postValue(NetworkResultWrapper.error(
                        apiMessage != null && !apiMessage.isEmpty()
                            ? apiMessage
                            : "Gửi lại OTP thất bại."
                    ));
                    return;
                }

                result.postValue(NetworkResultWrapper.success(Boolean.TRUE));
            }

            @Override
            public void onFailure(Call<BaseResponse<Void>> call, Throwable t) {
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
        authApiService.forgotPassword(request).enqueue(new Callback<BaseResponse<Void>>() {
            @Override
            public void onResponse(Call<BaseResponse<Void>> call, Response<BaseResponse<Void>> response) {
                if (!response.isSuccessful()) {
                    result.postValue(NetworkResultWrapper.error(extractHttpError(response), response.code()));
                    return;
                }

                BaseResponse<Void> baseResponse = response.body();
                if (baseResponse == null) {
                    result.postValue(NetworkResultWrapper.error("Phản hồi quên mật khẩu không hợp lệ."));
                    return;
                }

                if (!baseResponse.isSuccess()) {
                    String apiMessage = baseResponse.getMessage();
                    result.postValue(NetworkResultWrapper.error(
                        apiMessage != null && !apiMessage.isEmpty()
                            ? apiMessage
                            : "Không gửi được OTP."
                    ));
                    return;
                }

                result.postValue(NetworkResultWrapper.success(request.getEmail()));
            }

            @Override
            public void onFailure(Call<BaseResponse<Void>> call, Throwable t) {
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
        authApiService.verifyResetOtp(request).enqueue(new Callback<BaseResponse<Void>>() {
            @Override
            public void onResponse(Call<BaseResponse<Void>> call, Response<BaseResponse<Void>> response) {
                if (!response.isSuccessful()) {
                    result.postValue(NetworkResultWrapper.error(extractHttpError(response), response.code()));
                    return;
                }

                BaseResponse<Void> baseResponse = response.body();
                if (baseResponse == null) {
                    result.postValue(NetworkResultWrapper.error("Phản hồi xác thực OTP không hợp lệ."));
                    return;
                }

                if (!baseResponse.isSuccess()) {
                    String apiMessage = baseResponse.getMessage();
                    result.postValue(NetworkResultWrapper.error(
                        apiMessage != null && !apiMessage.isEmpty()
                            ? apiMessage
                            : "Xác thực OTP thất bại."
                    ));
                    return;
                }

                result.postValue(NetworkResultWrapper.success(request.getEmail()));
            }

            @Override
            public void onFailure(Call<BaseResponse<Void>> call, Throwable t) {
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
        authApiService.resetPassword(request).enqueue(new Callback<BaseResponse<Void>>() {
            @Override
            public void onResponse(Call<BaseResponse<Void>> call, Response<BaseResponse<Void>> response) {
                if (!response.isSuccessful()) {
                    result.postValue(NetworkResultWrapper.error(extractHttpError(response), response.code()));
                    return;
                }

                BaseResponse<Void> baseResponse = response.body();
                if (baseResponse == null) {
                    result.postValue(NetworkResultWrapper.error("Phản hồi đổi mật khẩu không hợp lệ."));
                    return;
                }

                if (!baseResponse.isSuccess()) {
                    String apiMessage = baseResponse.getMessage();
                    result.postValue(NetworkResultWrapper.error(
                        apiMessage != null && !apiMessage.isEmpty()
                            ? apiMessage
                            : "Đổi mật khẩu thất bại."
                    ));
                    return;
                }

                String apiMessage = baseResponse.getMessage();
                result.postValue(NetworkResultWrapper.success(
                    apiMessage != null && !apiMessage.isEmpty()
                        ? apiMessage
                        : "Đổi mật khẩu thành công"
                ));
            }

            @Override
            public void onFailure(Call<BaseResponse<Void>> call, Throwable t) {
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
