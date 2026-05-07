package com.drc.aidbridge.data.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.drc.aidbridge.data.mapper.UserMapper;
import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.data.remote.api.UserApiService;
import com.drc.aidbridge.data.remote.dto.request.ChangePasswordRequest;
import com.drc.aidbridge.data.remote.dto.request.UpdateProfileRequest;
import com.drc.aidbridge.data.remote.dto.response.AvatarUploadResponse;
import com.drc.aidbridge.data.remote.dto.response.BaseResponse;
import com.drc.aidbridge.data.remote.dto.response.UserDto;
import com.drc.aidbridge.domain.enums.UserRole;
import com.drc.aidbridge.domain.model.User;
import com.drc.aidbridge.domain.repository.UserRepository;
import com.drc.aidbridge.utils.TokenManager;

import javax.inject.Inject;
import javax.inject.Singleton;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@Singleton
public class UserRepositoryImpl extends BaseRepository implements UserRepository {

    private final UserApiService userApiService;
    private final TokenManager tokenManager;
    private final UserMapper userMapper;

    @Inject
    public UserRepositoryImpl(UserApiService userApiService,
                              TokenManager tokenManager,
                              UserMapper userMapper) {
        this.userApiService = userApiService;
        this.tokenManager = tokenManager;
        this.userMapper = userMapper;
    }

    @Override
    public LiveData<NetworkResultWrapper<User>> getCachedUser() {
        MutableLiveData<NetworkResultWrapper<User>> result = new MutableLiveData<>();
        result.postValue(NetworkResultWrapper.loading());

        User cachedUser = new User(
            safe(tokenManager.getUserId()),
            safe(tokenManager.getUserName()),
            safe(tokenManager.getUserEmail()),
            safe(tokenManager.getUserPhone()),
            safe(tokenManager.getUserAddress()),
            UserRole.fromStringSafe(tokenManager.getUserRole()),
            tokenManager.getUserAvatar(),
            tokenManager.isUserVerified(),
            tokenManager.getUserCreatedAt()
        );

        result.postValue(NetworkResultWrapper.success(cachedUser));
        return result;
    }

    @Override
    public LiveData<NetworkResultWrapper<User>> getCurrentUser() {
        MutableLiveData<NetworkResultWrapper<User>> result = new MutableLiveData<>();
        result.postValue(NetworkResultWrapper.loading());

        userApiService.getCurrentUser().enqueue(new Callback<BaseResponse<UserDto>>() {
            @Override
            public void onResponse(Call<BaseResponse<UserDto>> call,
                                   Response<BaseResponse<UserDto>> response) {
                if (!response.isSuccessful()) {
                    result.postValue(NetworkResultWrapper.error(extractHttpError(response), response.code()));
                    return;
                }

                BaseResponse<UserDto> baseResponse = response.body();
                if (baseResponse == null) {
                    result.postValue(NetworkResultWrapper.error("Phan hoi ho so nguoi dung khong hop le."));
                    return;
                }

                if (!baseResponse.isSuccess()) {
                    String apiMessage = baseResponse.getMessage();
                    result.postValue(NetworkResultWrapper.error(
                        apiMessage != null && !apiMessage.trim().isEmpty()
                            ? apiMessage
                            : "Khong the tai ho so nguoi dung."
                    ));
                    return;
                }

                UserDto userDto = baseResponse.getData();
                if (userDto == null) {
                    result.postValue(NetworkResultWrapper.error("Khong nhan duoc thong tin nguoi dung."));
                    return;
                }

                User user = userMapper.mapToDomain(userDto);
                cacheUser(user);
                result.postValue(NetworkResultWrapper.success(user));
            }

            @Override
            public void onFailure(Call<BaseResponse<UserDto>> call, Throwable t) {
                result.postValue(NetworkResultWrapper.error("Khong the tai ho so nguoi dung: " + safeMessage(t)));
            }
        });

        return result;
    }

    @Override
    public LiveData<NetworkResultWrapper<User>> updateProfile(String name, String phone, String address) {
        MutableLiveData<NetworkResultWrapper<User>> result = new MutableLiveData<>();
        result.postValue(NetworkResultWrapper.loading());

        UpdateProfileRequest request = new UpdateProfileRequest(name, phone, address);
        userApiService.updateProfile(request).enqueue(new Callback<BaseResponse<UserDto>>() {
            @Override
            public void onResponse(Call<BaseResponse<UserDto>> call,
                                   Response<BaseResponse<UserDto>> response) {
                if (!response.isSuccessful()) {
                    result.postValue(NetworkResultWrapper.error(extractHttpError(response), response.code()));
                    return;
                }

                BaseResponse<UserDto> baseResponse = response.body();
                if (baseResponse == null) {
                    result.postValue(NetworkResultWrapper.error("Phản hồi cập nhật hồ sơ không hợp lệ."));
                    return;
                }

                if (!baseResponse.isSuccess()) {
                    String apiMessage = baseResponse.getMessage();
                    result.postValue(NetworkResultWrapper.error(
                        apiMessage != null && !apiMessage.trim().isEmpty()
                            ? apiMessage
                            : "Cập nhật hồ sơ thất bại."
                    ));
                    return;
                }

                UserDto userDto = baseResponse.getData();
                User updatedUser;
                if (userDto != null) {
                    updatedUser = userMapper.mapToDomain(userDto);
                    tokenManager.updateUserInfo(
                        updatedUser.getName(),
                        updatedUser.getPhone(),
                        updatedUser.getEmail(),
                        updatedUser.getAvatarUrl(),
                        updatedUser.getAddress() != null ? updatedUser.getAddress() : address,
                        updatedUser.getCreatedAt()
                    );
                } else {
                    tokenManager.updateUserInfo(name, phone, null, null, address);
                    updatedUser = new User(
                        safe(tokenManager.getUserId()),
                        safe(name),
                        safe(tokenManager.getUserEmail()),
                        safe(phone),
                        safe(address),
                        UserRole.fromStringSafe(tokenManager.getUserRole()),
                        tokenManager.getUserAvatar(),
                        tokenManager.isUserVerified(),
                        tokenManager.getUserCreatedAt()
                    );
                }

                result.postValue(NetworkResultWrapper.success(updatedUser));
            }

            @Override
            public void onFailure(Call<BaseResponse<UserDto>> call, Throwable t) {
                result.postValue(NetworkResultWrapper.error("Cập nhật hồ sơ thất bại: " + safeMessage(t)));
            }
        });

        return result;
    }

    @Override
    public LiveData<NetworkResultWrapper<String>> changePassword(String currentPassword, String newPassword) {
        MutableLiveData<NetworkResultWrapper<String>> result = new MutableLiveData<>();
        result.postValue(NetworkResultWrapper.loading());

        ChangePasswordRequest request = new ChangePasswordRequest(currentPassword, newPassword);
        userApiService.changePassword(request).enqueue(new Callback<BaseResponse<String>>() {
            @Override
            public void onResponse(Call<BaseResponse<String>> call,
                                   Response<BaseResponse<String>> response) {
                if (!response.isSuccessful()) {
                    result.postValue(NetworkResultWrapper.error(extractHttpError(response), response.code()));
                    return;
                }

                BaseResponse<String> baseResponse = response.body();
                if (baseResponse == null) {
                    result.postValue(NetworkResultWrapper.error("Phản hồi đổi mật khẩu không hợp lệ."));
                    return;
                }

                if (!baseResponse.isSuccess()) {
                    String apiMessage = baseResponse.getMessage();
                    result.postValue(NetworkResultWrapper.error(
                        apiMessage != null && !apiMessage.trim().isEmpty()
                            ? apiMessage
                            : "Đổi mật khẩu thất bại."
                    ));
                    return;
                }

                String message = baseResponse.getMessage();
                result.postValue(NetworkResultWrapper.success(
                    message != null && !message.trim().isEmpty()
                        ? message
                        : "Đổi mật khẩu thành công."
                ));
            }

            @Override
            public void onFailure(Call<BaseResponse<String>> call, Throwable t) {
                result.postValue(NetworkResultWrapper.error("Đổi mật khẩu thất bại: " + safeMessage(t)));
            }
        });

        return result;
    }

    @Override
    public LiveData<NetworkResultWrapper<String>> uploadAvatar(MultipartBody.Part avatarPart) {
        MutableLiveData<NetworkResultWrapper<String>> result = new MutableLiveData<>();
        result.postValue(NetworkResultWrapper.loading());

        userApiService.uploadAvatar(avatarPart).enqueue(new Callback<BaseResponse<AvatarUploadResponse>>() {
            @Override
            public void onResponse(Call<BaseResponse<AvatarUploadResponse>> call,
                                   Response<BaseResponse<AvatarUploadResponse>> response) {
                if (!response.isSuccessful()) {
                    result.postValue(NetworkResultWrapper.error(extractHttpError(response), response.code()));
                    return;
                }

                BaseResponse<AvatarUploadResponse> baseResponse = response.body();
                if (baseResponse == null) {
                    result.postValue(NetworkResultWrapper.error("Phản hồi tải ảnh đại diện không hợp lệ."));
                    return;
                }

                if (!baseResponse.isSuccess()) {
                    String apiMessage = baseResponse.getMessage();
                    result.postValue(NetworkResultWrapper.error(
                        apiMessage != null && !apiMessage.trim().isEmpty()
                            ? apiMessage
                            : "Tải ảnh đại diện thất bại."
                    ));
                    return;
                }

                AvatarUploadResponse data = baseResponse.getData();
                String avatarUrl = data != null ? data.getAvatarUrl() : null;
                if (avatarUrl == null || avatarUrl.trim().isEmpty()) {
                    result.postValue(NetworkResultWrapper.error("Không nhận được đường dẫn ảnh đại diện mới."));
                    return;
                }

                tokenManager.updateUserInfo(null, null, null, avatarUrl.trim(), null);
                result.postValue(NetworkResultWrapper.success(avatarUrl.trim()));
            }

            @Override
            public void onFailure(Call<BaseResponse<AvatarUploadResponse>> call, Throwable t) {
                result.postValue(NetworkResultWrapper.error("Tải ảnh đại diện thất bại: " + safeMessage(t)));
            }
        });

        return result;
    }

    private String safe(String value) {
        return value != null ? value : "";
    }

    private void cacheUser(User user) {
        if (user == null) {
            return;
        }

        String role = user.getRole() != null ? user.getRole().name() : tokenManager.getUserRole();
        tokenManager.saveUserInfo(
            user.getId(),
            user.getName(),
            user.getEmail(),
            user.getPhone(),
            role,
            user.getAvatarUrl(),
            user.isVerified(),
            user.getCreatedAt()
        );

        if (user.getAddress() != null) {
            tokenManager.updateUserInfo(null, null, null, null, user.getAddress());
        }
    }
}
