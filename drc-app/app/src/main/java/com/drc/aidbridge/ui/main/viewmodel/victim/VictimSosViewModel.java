package com.drc.aidbridge.ui.main.viewmodel.victim;

import android.content.Context;
import android.net.Uri;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.domain.usecase.validation.ValidationResult;
import com.drc.aidbridge.domain.usecase.victim.UploadSosUseCase;
import com.drc.aidbridge.ui.base.BaseViewModel;
import com.drc.aidbridge.utils.ImageUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

@HiltViewModel
public class VictimSosViewModel extends BaseViewModel {

    private static final int DEFAULT_PEOPLE_COUNT = 1;
    private static final String DEFAULT_FULL_NAME = "Nạn nhân chưa xác định";
    private static final String DEFAULT_SEVERITY = "Trung bình";

    private final UploadSosUseCase uploadSosUseCase;
    private final ExecutorService uploadExecutor = Executors.newSingleThreadExecutor();

    private final MutableLiveData<SelfSosPayload> selfSosTrigger = new MutableLiveData<>();
    private final MutableLiveData<RelativeSosPayload> relativeSosTrigger = new MutableLiveData<>();

    private final LiveData<NetworkResultWrapper<String>> selfSosSource;
    private final LiveData<NetworkResultWrapper<String>> relativeSosSource;

    private final MutableLiveData<ValidationResult> validationError = new MutableLiveData<>();
    private final MediatorLiveData<NetworkResultWrapper<String>> submitSelfSosResult = new MediatorLiveData<>();
    private final MediatorLiveData<NetworkResultWrapper<String>> submitRelativeSosResult = new MediatorLiveData<>();

    private final Object tempFilesLock = new Object();
    private List<File> pendingSelfTempFiles = new ArrayList<>();

    @Inject
    public VictimSosViewModel(UploadSosUseCase uploadSosUseCase) {
        this.uploadSosUseCase = uploadSosUseCase;

        selfSosSource = Transformations.switchMap(selfSosTrigger, payload ->
            this.uploadSosUseCase.uploadSelfSos(
                payload.fullName,
                payload.peopleCount,
                payload.severity,
                payload.note,
                payload.latitude,
                payload.longitude,
                payload.imageParts
            )
        );

        relativeSosSource = Transformations.switchMap(relativeSosTrigger, payload ->
            this.uploadSosUseCase.uploadRelativeSos(
                payload.relativeName,
                payload.relativePhone,
                payload.relativeAddress,
                payload.severity,
                payload.latitude,
                payload.longitude
            )
        );

        submitSelfSosResult.addSource(selfSosSource, result -> {
            submitSelfSosResult.postValue(result);
            if (result != null && !result.isLoading()) {
                cleanupPendingSelfTempFiles();
            }
        });

        submitRelativeSosResult.addSource(relativeSosSource, submitRelativeSosResult::postValue);
    }

    public LiveData<NetworkResultWrapper<String>> getSubmitSelfSosResult() {
        return submitSelfSosResult;
    }

    public LiveData<NetworkResultWrapper<String>> getSubmitRelativeSosResult() {
        return submitRelativeSosResult;
    }

    public LiveData<ValidationResult> getValidationError() {
        return validationError;
    }

    public void submitSelfSos(Context context,
                              String fullName,
                              int peopleCount,
                              String severity,
                              String note,
                              double latitude,
                              double longitude,
                              List<Uri> imageUris) {
        ValidationResult validation = uploadSosUseCase.validateSelfSos(fullName, peopleCount, severity, note);
        if (!validation.isValid()) {
            validationError.postValue(validation);
            return;
        }

        validationError.postValue(ValidationResult.valid());
        submitSelfSosResult.postValue(NetworkResultWrapper.loading());

        uploadExecutor.execute(() -> {
            List<File> compressedFiles = new ArrayList<>();
            try {
                Context appContext = context.getApplicationContext();
                if (imageUris != null) {
                    for (Uri imageUri : imageUris) {
                        if (imageUri == null) {
                            continue;
                        }
                        compressedFiles.add(ImageUtils.compressScenePhoto(appContext, imageUri));
                    }
                }

                List<MultipartBody.Part> imageParts = createImageParts(compressedFiles);
                setPendingSelfTempFiles(compressedFiles);

                SelfSosPayload payload = new SelfSosPayload(
                    normalizeFullName(fullName),
                    peopleCount > 0 ? peopleCount : DEFAULT_PEOPLE_COUNT,
                    normalizeSeverity(severity),
                    note != null ? note.trim() : "",
                    latitude,
                    longitude,
                    imageParts
                );

                selfSosTrigger.postValue(payload);
            } catch (IOException exception) {
                cleanupFiles(compressedFiles);
                submitSelfSosResult.postValue(NetworkResultWrapper.error(
                    "Không thể nén ảnh hiện trường: " + safeMessage(exception)
                ));
            } catch (Exception exception) {
                cleanupFiles(compressedFiles);
                submitSelfSosResult.postValue(NetworkResultWrapper.error(
                    "Gửi SOS thất bại: " + safeMessage(exception)
                ));
            }
        });
    }

    public void submitRelativeSos(String relativeName,
                                  String relativePhone,
                                  String relativeAddress,
                                  String severity,
                                  double latitude,
                                  double longitude) {
        ValidationResult validation = uploadSosUseCase.validateRelativeSos(
            relativeName,
            relativeAddress,
            severity
        );
        if (!validation.isValid()) {
            validationError.setValue(validation);
            return;
        }

        validationError.setValue(ValidationResult.valid());
        submitRelativeSosResult.setValue(NetworkResultWrapper.loading());

        RelativeSosPayload payload = new RelativeSosPayload(
            relativeName != null ? relativeName.trim() : "",
            relativePhone != null ? relativePhone.trim() : "",
            relativeAddress != null ? relativeAddress.trim() : "",
            normalizeSeverity(severity),
            latitude,
            longitude
        );

        relativeSosTrigger.setValue(payload);
    }

    @Override
    protected void onCleared() {
        cleanupPendingSelfTempFiles();
        uploadExecutor.shutdownNow();
        super.onCleared();
    }

    private List<MultipartBody.Part> createImageParts(List<File> compressedFiles) {
        if (compressedFiles == null || compressedFiles.isEmpty()) {
            return Collections.emptyList();
        }

        List<MultipartBody.Part> parts = new ArrayList<>();
        for (File compressedFile : compressedFiles) {
            if (compressedFile == null || !compressedFile.exists()) {
                continue;
            }

            String mimeType = resolveMimeType(compressedFile.getName());
            RequestBody requestBody = RequestBody.create(compressedFile, MediaType.get(mimeType));
            parts.add(MultipartBody.Part.createFormData(
                "images",
                compressedFile.getName(),
                requestBody
            ));
        }

        return parts;
    }

    private String normalizeFullName(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) {
            return DEFAULT_FULL_NAME;
        }
        return fullName.trim();
    }

    private String normalizeSeverity(String severity) {
        if (severity == null || severity.trim().isEmpty()) {
            return DEFAULT_SEVERITY;
        }
        return severity.trim();
    }

    private String resolveMimeType(String fileName) {
        String lowerName = fileName != null ? fileName.toLowerCase() : "";
        if (lowerName.endsWith(".png")) {
            return "image/png";
        }
        if (lowerName.endsWith(".webp")) {
            return "image/webp";
        }
        return "image/jpeg";
    }

    private void setPendingSelfTempFiles(List<File> files) {
        synchronized (tempFilesLock) {
            pendingSelfTempFiles = new ArrayList<>(files);
        }
    }

    private void cleanupPendingSelfTempFiles() {
        List<File> filesToDelete;
        synchronized (tempFilesLock) {
            filesToDelete = pendingSelfTempFiles;
            pendingSelfTempFiles = new ArrayList<>();
        }
        cleanupFiles(filesToDelete);
    }

    private void cleanupFiles(List<File> files) {
        if (files == null || files.isEmpty()) {
            return;
        }

        for (File file : files) {
            if (file != null && file.exists()) {
                file.delete();
            }
        }
    }

    private String safeMessage(Throwable throwable) {
        String message = throwable != null ? throwable.getMessage() : null;
        if (message == null || message.trim().isEmpty()) {
            return "Lỗi không xác định";
        }
        return message.trim();
    }

    private static final class SelfSosPayload {
        final String fullName;
        final int peopleCount;
        final String severity;
        final String note;
        final double latitude;
        final double longitude;
        final List<MultipartBody.Part> imageParts;

        SelfSosPayload(String fullName,
                       int peopleCount,
                       String severity,
                       String note,
                       double latitude,
                       double longitude,
                       List<MultipartBody.Part> imageParts) {
            this.fullName = fullName;
            this.peopleCount = peopleCount;
            this.severity = severity;
            this.note = note;
            this.latitude = latitude;
            this.longitude = longitude;
            this.imageParts = imageParts;
        }
    }

    private static final class RelativeSosPayload {
        final String relativeName;
        final String relativePhone;
        final String relativeAddress;
        final String severity;
        final double latitude;
        final double longitude;

        RelativeSosPayload(String relativeName,
                           String relativePhone,
                           String relativeAddress,
                           String severity,
                           double latitude,
                           double longitude) {
            this.relativeName = relativeName;
            this.relativePhone = relativePhone;
            this.relativeAddress = relativeAddress;
            this.severity = severity;
            this.latitude = latitude;
            this.longitude = longitude;
        }
    }
}
