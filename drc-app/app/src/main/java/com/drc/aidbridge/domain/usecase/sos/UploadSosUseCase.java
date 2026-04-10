package com.drc.aidbridge.domain.usecase.sos;

import androidx.lifecycle.LiveData;

import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.domain.repository.SosRepository;

import java.util.List;

import javax.inject.Inject;

import okhttp3.MultipartBody;

/**
 * UploadSosUseCase orchestrates SOS submission for victim self and relative flows.
 */
public class UploadSosUseCase {

    private final SosRepository sosRepository;

    @Inject
    public UploadSosUseCase(SosRepository sosRepository) {
        this.sosRepository = sosRepository;
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
