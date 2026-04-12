package com.drc.aidbridge.domain.usecase.victim;

import androidx.lifecycle.LiveData;

import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.domain.repository.victim.VictimSosRepository;
import com.drc.aidbridge.domain.usecase.validation.ValidationResult;
import com.drc.aidbridge.domain.usecase.validation.VictimSosInputValidator;

import javax.inject.Inject;

/**
 * UploadSosUseCase orchestrates SOS submission for victim self and relative flows.
 */
public class UploadSosUseCase {

    private final VictimSosRepository victimSosRepository;
    private final VictimSosInputValidator victimSosInputValidator;

    @Inject
    public UploadSosUseCase(VictimSosRepository victimSosRepository,
                            VictimSosInputValidator victimSosInputValidator) {
        this.victimSosRepository = victimSosRepository;
        this.victimSosInputValidator = victimSosInputValidator;
    }

    public ValidationResult validateSelfSos(String fullName,
                                                int peopleCount,
                                                String severity,
                                                String note) {
        return victimSosInputValidator.validateSelfSos(fullName, peopleCount, severity, note);
    }

    public ValidationResult validateRelativeSos(String relativeName,
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
                                                                 String firstImageUrl) {
        return victimSosRepository.uploadSelfSos(
            fullName,
            peopleCount,
            severity,
            note,
            latitude,
            longitude,
            firstImageUrl
        );
    }

    public LiveData<NetworkResultWrapper<String>> uploadRelativeSos(String relativeName,
                                                                     String relativePhone,
                                                                     String relativeAddress,
                                                                     String severity,
                                                                     double latitude,
                                                                     double longitude) {
        return victimSosRepository.uploadRelativeSos(
            relativeName,
            relativePhone,
            relativeAddress,
            severity,
            latitude,
            longitude
        );
    }
}
