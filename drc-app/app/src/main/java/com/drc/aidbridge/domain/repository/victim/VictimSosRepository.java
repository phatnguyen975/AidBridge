package com.drc.aidbridge.domain.repository.victim;

import androidx.lifecycle.LiveData;

import com.drc.aidbridge.data.remote.NetworkResultWrapper;

import java.util.List;

import okhttp3.MultipartBody;

public interface VictimSosRepository {

    LiveData<NetworkResultWrapper<String>> uploadSelfSos(String fullName,
                                                         int peopleCount,
                                                         String severity,
                                                         String note,
                                                         double latitude,
                                                         double longitude,
                                                         List<MultipartBody.Part> imageParts);

    LiveData<NetworkResultWrapper<String>> uploadRelativeSos(String relativeName,
                                                             String relativePhone,
                                                             String relativeAddress,
                                                             String severity,
                                                             double latitude,
                                                             double longitude);
}
