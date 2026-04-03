package com.drc.aidbridge.ui.main.viewmodel.victim;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;

import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.domain.usecase.user.UploadAvatarUseCase;
import com.drc.aidbridge.utils.ImageUtils;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;

import okhttp3.MultipartBody;

final class AvatarUploadCoordinator {

    private AvatarUploadCoordinator() {
    }

    static void uploadAvatar(Context context,
                             Uri imageUri,
                             ExecutorService uploadExecutor,
                             Handler mainHandler,
                             MediatorLiveData<NetworkResultWrapper<String>> uploadAvatarResult,
                             UploadAvatarUseCase uploadAvatarUseCase) {
        uploadAvatarResult.postValue(NetworkResultWrapper.loading());

        uploadExecutor.execute(() -> {
            File compressedFile = null;
            try {
                compressedFile = ImageUtils.compressAvatar(context.getApplicationContext(), imageUri);
                MultipartBody.Part avatarPart = ImageUtils.createAvatarMultipart(compressedFile);
                LiveData<NetworkResultWrapper<String>> source = uploadAvatarUseCase.execute(avatarPart);
                File finalCompressedFile = compressedFile;

                mainHandler.post(() -> uploadAvatarResult.addSource(source, result -> {
                    uploadAvatarResult.postValue(result);
                    if (result != null && !result.isLoading()) {
                        uploadAvatarResult.removeSource(source);
                        if (finalCompressedFile.exists()) {
                            // Best-effort cleanup for temporary compressed files.
                            finalCompressedFile.delete();
                        }
                    }
                }));
            } catch (IOException exception) {
                cleanup(compressedFile);
                uploadAvatarResult.postValue(NetworkResultWrapper.error(
                    "Không thể nén ảnh đại diện: " + safeMessage(exception)
                ));
            } catch (Exception exception) {
                cleanup(compressedFile);
                uploadAvatarResult.postValue(NetworkResultWrapper.error(
                    "Tải ảnh đại diện thất bại: " + safeMessage(exception)
                ));
            }
        });
    }

    private static void cleanup(File file) {
        if (file != null && file.exists()) {
            file.delete();
        }
    }

    private static String safeMessage(Throwable throwable) {
        String message = throwable.getMessage();
        if (message == null || message.trim().isEmpty()) {
            return "Lỗi không xác định";
        }
        return message.trim();
    }
}