package com.drc.aidbridge.ui.main.viewmodel.victim;

import android.content.Context;
import android.net.Uri;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.domain.model.User;
import com.drc.aidbridge.domain.usecase.user.GetCachedUserUseCase;
import com.drc.aidbridge.domain.usecase.validation.ValidationResult;
import com.drc.aidbridge.domain.usecase.victim.UploadSosUseCase;
import com.drc.aidbridge.ui.base.BaseViewModel;
import com.drc.aidbridge.utils.ImageUtils;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class VictimSosViewModel extends BaseViewModel {

    private static final int DEFAULT_PEOPLE_COUNT = 1;
    private static final String DEFAULT_FULL_NAME = "Nạn nhân chưa xác định";
    private static final String DEFAULT_SEVERITY = "Nguy kịch";

    private final UploadSosUseCase uploadSosUseCase;
    private final GetCachedUserUseCase getCachedUserUseCase;
    private final ExecutorService imageProcessingExecutor = Executors.newSingleThreadExecutor();

    private final MutableLiveData<Long> loadCachedUserTrigger = new MutableLiveData<>();
    private final MutableLiveData<SelfSosPayload> selfSosTrigger = new MutableLiveData<>();
    private final MutableLiveData<RelativeSosPayload> relativeSosTrigger = new MutableLiveData<>();

    private final LiveData<NetworkResultWrapper<User>> cachedUserSource;
    private final LiveData<NetworkResultWrapper<String>> selfSosSource;
    private final LiveData<NetworkResultWrapper<String>> relativeSosSource;

    private final MediatorLiveData<NetworkResultWrapper<User>> cachedUserResult = new MediatorLiveData<>();
    private final MutableLiveData<ValidationResult> validationError = new MutableLiveData<>();
    private final MediatorLiveData<NetworkResultWrapper<String>> submitSelfSosResult = new MediatorLiveData<>();
    private final MediatorLiveData<NetworkResultWrapper<String>> submitRelativeSosResult = new MediatorLiveData<>();

    @Inject
    public VictimSosViewModel(UploadSosUseCase uploadSosUseCase,
                              GetCachedUserUseCase getCachedUserUseCase) {
        this.uploadSosUseCase = uploadSosUseCase;
        this.getCachedUserUseCase = getCachedUserUseCase;

        cachedUserSource = Transformations.switchMap(
            loadCachedUserTrigger,
            ignored -> this.getCachedUserUseCase.execute()
        );

        selfSosSource = Transformations.switchMap(selfSosTrigger, payload ->
            this.uploadSosUseCase.uploadSelfSos(
                payload.fullName,
                payload.peopleCount,
                payload.severity,
                payload.note,
                payload.latitude,
                payload.longitude,
                payload.firstImageUrl
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

        submitSelfSosResult.addSource(selfSosSource, submitSelfSosResult::postValue);

        submitRelativeSosResult.addSource(relativeSosSource, submitRelativeSosResult::postValue);
        cachedUserResult.addSource(cachedUserSource, cachedUserResult::postValue);
    }

    public LiveData<NetworkResultWrapper<User>> getCachedUserResult() {
        return cachedUserResult;
    }

    public void loadCachedUser() {
        loadCachedUserTrigger.setValue(System.currentTimeMillis());
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

        Context appContext = context.getApplicationContext();
        imageProcessingExecutor.execute(() -> {
            try {
                SelfSosPayload payload = new SelfSosPayload(
                    normalizeFullName(fullName),
                    peopleCount > 0 ? peopleCount : DEFAULT_PEOPLE_COUNT,
                    normalizeSeverity(severity),
                    note != null ? note.trim() : "",
                    latitude,
                    longitude,
                    extractFirstImageDataUrl(appContext, imageUris)
                );

                selfSosTrigger.postValue(payload);
            } catch (IOException exception) {
                submitSelfSosResult.postValue(NetworkResultWrapper.error(
                    "Không thể xử lý ảnh hiện trường: " + safeMessage(exception)
                ));
            } catch (Exception exception) {
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

    private String extractFirstImageDataUrl(Context context, List<Uri> imageUris) throws IOException {
        if (imageUris == null || imageUris.isEmpty()) {
            return "";
        }

        for (Uri uri : imageUris) {
            if (uri != null) {
                String dataUrl = ImageUtils.createScenePhotoDataUrl(context, uri);
                if (dataUrl != null && !dataUrl.trim().isEmpty()) {
                    return dataUrl.trim();
                }
            }
        }

        return "";
    }

    private String safeMessage(Throwable throwable) {
        String message = throwable != null ? throwable.getMessage() : null;
        if (message == null || message.trim().isEmpty()) {
            return "Lỗi không xác định";
        }
        return message.trim();
    }

    @Override
    protected void onCleared() {
        imageProcessingExecutor.shutdownNow();
        super.onCleared();
    }

    private static final class SelfSosPayload {
        final String fullName;
        final int peopleCount;
        final String severity;
        final String note;
        final double latitude;
        final double longitude;
        final String firstImageUrl;

        SelfSosPayload(String fullName,
                       int peopleCount,
                       String severity,
                       String note,
                       double latitude,
                       double longitude,
                       String firstImageUrl) {
            this.fullName = fullName;
            this.peopleCount = peopleCount;
            this.severity = severity;
            this.note = note;
            this.latitude = latitude;
            this.longitude = longitude;
            this.firstImageUrl = firstImageUrl;
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
