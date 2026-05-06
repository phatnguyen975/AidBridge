package com.drc.aidbridge.domain.repository;

import androidx.lifecycle.LiveData;

import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.domain.model.User;

import okhttp3.MultipartBody;

public interface UserRepository {

    LiveData<NetworkResultWrapper<User>> getCachedUser();

    LiveData<NetworkResultWrapper<User>> getCurrentUser();

    LiveData<NetworkResultWrapper<User>> updateProfile(String name, String phone, String address);

    LiveData<NetworkResultWrapper<String>> changePassword(String currentPassword, String newPassword);

    LiveData<NetworkResultWrapper<String>> uploadAvatar(MultipartBody.Part avatarPart);
}
