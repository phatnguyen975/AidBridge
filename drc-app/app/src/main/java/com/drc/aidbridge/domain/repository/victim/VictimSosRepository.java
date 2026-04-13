package com.drc.aidbridge.domain.repository.victim;

import androidx.lifecycle.LiveData;

import com.drc.aidbridge.data.remote.NetworkResultWrapper;

public interface VictimSosRepository {

    LiveData<NetworkResultWrapper<String>> uploadSelfSos(String fullName,
                                                         int peopleCount,
                                                         String severity,
                                                         String note,
                                                         double latitude,
                                                         double longitude,
                                                         String firstImageUrl);

    LiveData<NetworkResultWrapper<String>> uploadRelativeSos(String relativeName,
                                                             String relativePhone,
                                                             String relativeAddress,
                                                             String severity,
                                                             double latitude,
                                                             double longitude);
}
