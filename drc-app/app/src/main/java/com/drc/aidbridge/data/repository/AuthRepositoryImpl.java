package com.drc.aidbridge.data.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.drc.aidbridge.data.remote.api.AuthApiService;
import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.data.remote.dto.response.AuthResponse;
import com.drc.aidbridge.data.remote.dto.response.BaseResponse;
import com.drc.aidbridge.data.remote.dto.request.LoginRequest;
import com.drc.aidbridge.data.remote.dto.request.LogoutRequest;
import com.drc.aidbridge.data.remote.dto.request.OtpVerifyRequest;
import com.drc.aidbridge.data.remote.dto.request.RequestOtpRequest;
import com.drc.aidbridge.data.remote.dto.request.RegisterRequest;
import com.drc.aidbridge.data.remote.dto.request.ResetPasswordRequest;
import com.drc.aidbridge.data.remote.dto.request.UpdateFcmTokenRequest;
import com.drc.aidbridge.domain.model.User;
import com.drc.aidbridge.domain.repository.AuthRepository;
import com.drc.aidbridge.utils.TokenManager;
import com.drc.aidbridge.data.mapper.UserMapper;

import javax.inject.Inject;
import javax.inject.Singleton;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * AuthRepositoryImpl — concrete implementation of AuthRepository that interacts
 * with the AuthApiService.
 * It handles API calls, response parsing, error handling, and token management.
 * ViewModels depend on the AuthRepository interface, so they remain decoupled
 * from the actual data-fetching logic.
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
                            apiMessage != null && !apiMessage.trim().isEmpty()
                                    ? apiMessage
                                    : "Đăng nhập thất bại."));
                    return;
                }

                AuthResponse data = baseResponse.getData();
                if (data == null || data.getUser() == null) {
                    String apiMessage = baseResponse.getMessage();
                    result.postValue(NetworkResultWrapper.error(
                            apiMessage != null && !apiMessage.trim().isEmpty()
                                    ? apiMessage
                                    : "Phản hồi đăng nhập không hợp lệ."));
                    return;
                }

                persistAuthData(data);
                User user = userMapper.mapToDomain(data.getUser());
                tokenManager.saveUserInfo(
                        user.getId(),
                        user.getName(),
                        user.getEmail(),
                        user.getPhone(),
                        user.getRole().name(),
                        user.getAvatarUrl(),
                        user.isVerified());
                result.postValue(NetworkResultWrapper.success(user));
            }

            @Override
            public void onFailure(Call<BaseResponse<AuthResponse>> call, Throwable t) {
                result.postValue(NetworkResultWrapper.error("Không thể kết nối máy chủ: " + safeMessage(t)));
            }
        });

        return result;
    }

    @Override
    public LiveData<NetworkResultWrapper<User>> register(RegisterRequest request) {
        MutableLiveData<NetworkResultWrapper<User>> result = new MutableLiveData<>();
        result.postValue(NetworkResultWrapper.loading());

        authApiService.register(request).enqueue(new Callback<BaseResponse<AuthResponse>>() {
            @Override
            public void onResponse(Call<BaseResponse<AuthResponse>> call,
                    Response<BaseResponse<AuthResponse>> response) {
                // 1. HTTP-level success check
                if (!response.isSuccessful()) {
                    result.postValue(NetworkResultWrapper.error(extractHttpError(response), response.code()));
                    return;
                }

                // 2. Body null-safety check
                BaseResponse<AuthResponse> baseResponse = response.body();
                if (baseResponse == null) {
                    result.postValue(NetworkResultWrapper.error("Phản hồi đăng ký không hợp lệ."));
                    return;
                }

                // 3. API-level business success check
                if (!baseResponse.isSuccess()) {
                    String apiMessage = baseResponse.getMessage();
                    result.postValue(NetworkResultWrapper.error(
                            apiMessage != null && !apiMessage.trim().isEmpty()
                                    ? apiMessage
                                    : "Đăng ký thất bại."));
                    return;
                }

                // 4. Extract data payload and map user DTO to domain model
                AuthResponse data = baseResponse.getData();
                if (data == null || data.getUser() == null) {
                    String apiMessage = baseResponse.getMessage();
                    result.postValue(NetworkResultWrapper.error(
                            apiMessage != null && !apiMessage.trim().isEmpty()
                                    ? apiMessage
                                    : "Phản hồi đăng ký không hợp lệ."));
                    return;
                }

                User user = userMapper.mapToDomain(data.getUser());
                if (user == null) {
                    result.postValue(NetworkResultWrapper.error("Không thể ánh xạ thông tin người dùng."));
                    return;
                }

                // Persist access/refresh tokens and local user metadata for session bootstrap.
                persistAuthData(data);
                tokenManager.saveUserInfo(
                        user.getId(),
                        user.getName(),
                        user.getEmail(),
                        user.getPhone(),
                        user.getRole().name(),
                        user.getAvatarUrl(),
                        user.isVerified());

                result.postValue(NetworkResultWrapper.success(user));
            }

            @Override
            public void onFailure(Call<BaseResponse<AuthResponse>> call, Throwable t) {
                result.postValue(NetworkResultWrapper.error("Đăng ký thất bại: " + safeMessage(t)));
            }
        });

        return result;
    }

    @Override
    public LiveData<NetworkResultWrapper<AuthResponse>> verifyOtp(OtpVerifyRequest request) {
        MutableLiveData<NetworkResultWrapper<AuthResponse>> result = new MutableLiveData<>();
        result.postValue(NetworkResultWrapper.loading());

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
                            apiMessage != null && !apiMessage.trim().isEmpty()
                                    ? apiMessage
                                    : "Xác thực OTP thất bại."));
                    return;
                }

                AuthResponse data = baseResponse.getData();
                if (data == null) {
                    String apiMessage = baseResponse.getMessage();
                    result.postValue(NetworkResultWrapper.error(
                            apiMessage != null && !apiMessage.trim().isEmpty()
                                    ? apiMessage
                                    : "Phản hồi xác thực OTP không hợp lệ."));
                    return;
                }

                result.postValue(NetworkResultWrapper.success(data));
            }

            @Override
            public void onFailure(Call<BaseResponse<AuthResponse>> call, Throwable t) {
                result.postValue(NetworkResultWrapper.error("Xác thực OTP thất bại: " + safeMessage(t)));
            }
        });

        return result;
    }

    @Override
    public LiveData<NetworkResultWrapper<Boolean>> resendOtp(RequestOtpRequest request) {
        MutableLiveData<NetworkResultWrapper<Boolean>> result = new MutableLiveData<>();
        result.postValue(NetworkResultWrapper.loading());

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
                            apiMessage != null && !apiMessage.trim().isEmpty()
                                    ? apiMessage
                                    : "Gửi lại OTP thất bại."));
                    return;
                }

                result.postValue(NetworkResultWrapper.success(Boolean.TRUE));
            }

            @Override
            public void onFailure(Call<BaseResponse<Void>> call, Throwable t) {
                result.postValue(NetworkResultWrapper.error("Gửi lại OTP thất bại: " + safeMessage(t)));
            }
        });

        return result;
    }

    @Override
    public LiveData<NetworkResultWrapper<String>> forgotPassword(RequestOtpRequest request) {
        MutableLiveData<NetworkResultWrapper<String>> result = new MutableLiveData<>();
        result.postValue(NetworkResultWrapper.loading());

        authApiService.forgotPassword(request).enqueue(new Callback<BaseResponse<Void>>() {
            @Override
            public void onResponse(Call<BaseResponse<Void>> call,
                    Response<BaseResponse<Void>> response) {
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
                                    : "Không gửi được OTP."));
                    return;
                }

                String apiMessage = baseResponse.getMessage();
                result.postValue(NetworkResultWrapper.success(
                        apiMessage != null && !apiMessage.trim().isEmpty()
                                ? apiMessage
                                : "Đã gửi mã OTP. Vui lòng kiểm tra email của bạn."));
            }

            @Override
            public void onFailure(Call<BaseResponse<Void>> call, Throwable t) {
                result.postValue(NetworkResultWrapper.error("Không gửi được OTP: " + safeMessage(t)));
            }
        });

        return result;
    }

    @Override
    public LiveData<NetworkResultWrapper<String>> verifyResetOtp(OtpVerifyRequest request) {
        MutableLiveData<NetworkResultWrapper<String>> result = new MutableLiveData<>();
        result.postValue(NetworkResultWrapper.loading());

        authApiService.verifyResetOtp(request).enqueue(new Callback<BaseResponse<AuthResponse>>() {
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
                                    : "Xác thực OTP thất bại."));
                    return;
                }

                AuthResponse data = baseResponse.getData();
                if (data == null) {
                    String apiMessage = baseResponse.getMessage();
                    result.postValue(NetworkResultWrapper.error(
                            apiMessage != null && !apiMessage.trim().isEmpty()
                                    ? apiMessage
                                    : "Phản hồi xác thực OTP không hợp lệ."));
                    return;
                }

                // Reset-password flow only validates OTP; tokens from this endpoint are
                // ignored.
                result.postValue(NetworkResultWrapper.success(request.getOtpCode()));
            }

            @Override
            public void onFailure(Call<BaseResponse<AuthResponse>> call, Throwable t) {
                result.postValue(NetworkResultWrapper.error("Xác thực OTP thất bại: " + safeMessage(t)));
            }
        });

        return result;
    }

    @Override
    public LiveData<NetworkResultWrapper<String>> resetPassword(ResetPasswordRequest request) {
        MutableLiveData<NetworkResultWrapper<String>> result = new MutableLiveData<>();
        result.postValue(NetworkResultWrapper.loading());

        authApiService.resetPassword(request).enqueue(new Callback<BaseResponse<Void>>() {
            @Override
            public void onResponse(Call<BaseResponse<Void>> call,
                    Response<BaseResponse<Void>> response) {
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
                                    : "Đổi mật khẩu thất bại."));
                    return;
                }

                String apiMessage = baseResponse.getMessage();
                result.postValue(NetworkResultWrapper.success(
                        apiMessage != null && !apiMessage.isEmpty()
                                ? apiMessage
                                : "Đổi mật khẩu thành công"));
            }

            @Override
            public void onFailure(Call<BaseResponse<Void>> call, Throwable t) {
                result.postValue(NetworkResultWrapper.error("Đổi mật khẩu thất bại: " + safeMessage(t)));
            }
        });

        return result;
    }

    @Override
    public LiveData<NetworkResultWrapper<Boolean>> logout(String refreshToken) {
        MutableLiveData<NetworkResultWrapper<Boolean>> result = new MutableLiveData<>();
        result.postValue(NetworkResultWrapper.loading());

        // Always clear local session immediately so UI can return to auth shell
        // deterministically.
        tokenManager.clearAll();
        result.postValue(NetworkResultWrapper.success(Boolean.TRUE));

        if (refreshToken == null || refreshToken.trim().isEmpty()) {
            return result;
        }

        authApiService.logout(new LogoutRequest(refreshToken.trim())).enqueue(new Callback<BaseResponse<Void>>() {
            @Override
            public void onResponse(Call<BaseResponse<Void>> call, Response<BaseResponse<Void>> response) {
            }

            @Override
            public void onFailure(Call<BaseResponse<Void>> call, Throwable t) {
            }
        });

        return result;
    }

    @Override
    public void updateFcmToken(String deviceId, String fcmToken) {
        if (deviceId == null || deviceId.trim().isEmpty() || fcmToken == null || fcmToken.trim().isEmpty()) {
            return;
        }

        String accessToken = tokenManager.getAccessToken();
        if (accessToken == null || accessToken.trim().isEmpty()) {
            return;
        }

        String authorization = "Bearer " + accessToken.trim();
        UpdateFcmTokenRequest request = new UpdateFcmTokenRequest(deviceId.trim(), fcmToken.trim());
        authApiService.updateFcmToken(authorization, request).enqueue(new Callback<BaseResponse<Void>>() {
            @Override
            public void onResponse(Call<BaseResponse<Void>> call, Response<BaseResponse<Void>> response) {
            }

            @Override
            public void onFailure(Call<BaseResponse<Void>> call, Throwable t) {
            }
        });
    }

    private void persistAuthData(AuthResponse body) {
        if (body.getAccessToken() != null && body.getRefreshToken() != null) {
            tokenManager.saveTokens(body.getAccessToken(), body.getRefreshToken());
        }
    }
}
