package com.drc.aidbridge.domain.usecase.victim;

import androidx.lifecycle.LiveData;

import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.domain.repository.SosRepository;
import com.drc.aidbridge.domain.usecase.validation.AuthValidationResult;
import com.drc.aidbridge.domain.usecase.validation.VictimSosInputValidator;

import java.util.List;

import javax.inject.Inject;

import okhttp3.MultipartBody;

/**
 * UploadSosUseCase orchestrates SOS submission for victim self and relative flows.
 */
public class UploadSosUseCase {

    private final SosRepository sosRepository;
    private final VictimSosInputValidator victimSosInputValidator;

    @Inject
    public UploadSosUseCase(SosRepository sosRepository,
                            VictimSosInputValidator victimSosInputValidator) {
        this.sosRepository = sosRepository;
        this.victimSosInputValidator = victimSosInputValidator;
    }

    public AuthValidationResult validateSelfSos(String fullName,
                                                int peopleCount,
                                                String severity,
                                                String note) {
        return victimSosInputValidator.validateSelfSos(fullName, peopleCount, severity, note);
    }

    public AuthValidationResult validateRelativeSos(String relativeName,
                                                    String relativeAddress,
                                                    String severity) {
        return victimSosInputValidator.validateRelativeSos(relativeName, relativeAddress, severity);
    }

    public LiveData<NetworkResultWrapper<String>> uploadSelfSos(String fullName,
                                                                 int peopleCount,
                                                                 String severity,
                                                                 String note,
                                                                 double latitude,
                                                                 double longitude,
                                                                 List<MultipartBody.Part> imageParts) {
        return sosRepository.uploadSelfSos(
            fullName,
            peopleCount,
            severity,
            note,
            latitude,
            longitude,
            imageParts
        );
    }

    public LiveData<NetworkResultWrapper<String>> uploadRelativeSos(String relativeName,
                                                                     String relativePhone,
                                                                     String relativeAddress,
                                                                     String severity,
                                                                     double latitude,
                                                                     double longitude) {
        return sosRepository.uploadRelativeSos(
            relativeName,
            relativePhone,
            relativeAddress,
            severity,
            latitude,
            longitude
        );
    }
}
