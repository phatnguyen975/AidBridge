package com.drc.aidbridge.ui.auth.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.domain.usecase.validation.ValidationResult;
import com.drc.aidbridge.domain.usecase.victim.UploadSosUseCase;
import com.drc.aidbridge.ui.base.BaseViewModel;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class GuestSosViewModel extends BaseViewModel {

    private static final String DEFAULT_FULL_NAME = "Người bị nạn";
    private static final int DEFAULT_PEOPLE_COUNT = 1;
    private static final String DEFAULT_SEVERITY = "Nguy kịch";

    private final UploadSosUseCase uploadSosUseCase;

    private final MutableLiveData<GuestSosPayload> submitTrigger = new MutableLiveData<>();
    private final LiveData<NetworkResultWrapper<String>> submitSource;

    private final MutableLiveData<ValidationResult> validationError = new MutableLiveData<>();
    private final MediatorLiveData<NetworkResultWrapper<String>> submitResult = new MediatorLiveData<>();

    @Inject
    public GuestSosViewModel(UploadSosUseCase uploadSosUseCase) {
        this.uploadSosUseCase = uploadSosUseCase;

        submitSource = Transformations.switchMap(submitTrigger, payload ->
            this.uploadSosUseCase.uploadSelfSos(
                payload.fullName,
                payload.peopleCount,
                payload.severity,
                payload.note,
                payload.latitude,
                payload.longitude,
                ""
            )
        );

        submitResult.addSource(submitSource, submitResult::postValue);
    }

    public LiveData<ValidationResult> getValidationError() {
        return validationError;
    }

    public LiveData<NetworkResultWrapper<String>> getSubmitResult() {
        return submitResult;
    }

    public void submitSos(String fullName,
                          int peopleCount,
                          String severity,
                          String note,
                          double latitude,
                          double longitude) {
        ValidationResult validation = uploadSosUseCase.validateSelfSos(fullName, peopleCount, severity, note);
        if (!validation.isValid()) {
            validationError.setValue(validation);
            return;
        }

        validationError.setValue(ValidationResult.valid());
        submitResult.setValue(NetworkResultWrapper.loading());

        submitTrigger.setValue(new GuestSosPayload(
            normalizeFullName(fullName),
            peopleCount > 0 ? peopleCount : DEFAULT_PEOPLE_COUNT,
            normalizeSeverity(severity),
            note != null ? note.trim() : "",
            latitude,
            longitude
        ));
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

    private static final class GuestSosPayload {
        final String fullName;
        final int peopleCount;
        final String severity;
        final String note;
        final double latitude;
        final double longitude;

        GuestSosPayload(String fullName,
                        int peopleCount,
                        String severity,
                        String note,
                        double latitude,
                        double longitude) {
            this.fullName = fullName;
            this.peopleCount = peopleCount;
            this.severity = severity;
            this.note = note;
            this.latitude = latitude;
            this.longitude = longitude;
        }
    }
}
