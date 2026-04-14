package com.drc.aidbridge.ui.main.viewmodel.volunteer;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.domain.model.VolunteerMission;
import com.drc.aidbridge.domain.usecase.mission.AcceptVolunteerMissionUseCase;
import com.drc.aidbridge.domain.usecase.mission.GetVolunteerMissionUseCase;
import com.drc.aidbridge.domain.usecase.mission.RejectVolunteerMissionUseCase;
import com.drc.aidbridge.ui.base.BaseViewModel;

import java.time.Instant;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class VolunteerTaskViewModel extends BaseViewModel {

    private final GetVolunteerMissionUseCase getVolunteerMissionUseCase;
    private final AcceptVolunteerMissionUseCase acceptVolunteerMissionUseCase;
    private final RejectVolunteerMissionUseCase rejectVolunteerMissionUseCase;

    private final MutableLiveData<Boolean> isMissionAccepted = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> isMissionIgnored = new MutableLiveData<>(false);
    private final MutableLiveData<String> currentMissionType = new MutableLiveData<>(null);
    private final MutableLiveData<Integer> currentDeliveryStep = new MutableLiveData<>(1);
    private final MutableLiveData<VolunteerMission> pendingMission = new MutableLiveData<>(null);

    private final MediatorLiveData<NetworkResultWrapper<VolunteerMission>> pendingMissionResult =
            new MediatorLiveData<>();
    private final MediatorLiveData<NetworkResultWrapper<VolunteerMission>> acceptResult =
            new MediatorLiveData<>();
    private final MediatorLiveData<NetworkResultWrapper<Boolean>> rejectResult =
            new MediatorLiveData<>();

    @Nullable
    private DispatchContext currentDispatchContext;

    @Inject
    public VolunteerTaskViewModel(GetVolunteerMissionUseCase getVolunteerMissionUseCase,
                                  AcceptVolunteerMissionUseCase acceptVolunteerMissionUseCase,
                                  RejectVolunteerMissionUseCase rejectVolunteerMissionUseCase) {
        this.getVolunteerMissionUseCase = getVolunteerMissionUseCase;
        this.acceptVolunteerMissionUseCase = acceptVolunteerMissionUseCase;
        this.rejectVolunteerMissionUseCase = rejectVolunteerMissionUseCase;
    }

    public LiveData<Boolean> getIsMissionAccepted() {
        return isMissionAccepted;
    }

    public LiveData<Boolean> getIsMissionIgnored() {
        return isMissionIgnored;
    }

    public LiveData<String> getCurrentMissionType() {
        return currentMissionType;
    }

    public LiveData<Integer> getCurrentDeliveryStep() {
        return currentDeliveryStep;
    }

    public LiveData<VolunteerMission> getPendingMission() {
        return pendingMission;
    }

    public LiveData<NetworkResultWrapper<VolunteerMission>> getPendingMissionResult() {
        return pendingMissionResult;
    }

    public LiveData<NetworkResultWrapper<VolunteerMission>> getAcceptResult() {
        return acceptResult;
    }

    public LiveData<NetworkResultWrapper<Boolean>> getRejectResult() {
        return rejectResult;
    }

    public void setCurrentDeliveryStep(int step) {
        currentDeliveryStep.setValue(step);
    }

    public void openDispatchRequest(String missionId,
                                    String dispatchAttemptId,
                                    @Nullable String missionType,
                                    @Nullable String expiresAtIso) {
        currentDispatchContext = new DispatchContext(
                normalize(missionId),
                normalize(dispatchAttemptId),
                normalizeMissionType(missionType),
                parseInstant(expiresAtIso)
        );
        currentMissionType.setValue(currentDispatchContext.missionType);
        isMissionAccepted.setValue(false);
        isMissionIgnored.setValue(false);
        currentDeliveryStep.setValue(1);
        pendingMission.setValue(null);
        loadPendingMission(currentDispatchContext.missionId);
    }

    public boolean hasPendingDispatch() {
        return currentDispatchContext != null;
    }

    public void acceptPendingMission() {
        if (currentDispatchContext == null) {
            acceptResult.setValue(NetworkResultWrapper.error("Không tìm thấy nhiệm vụ điều phối để chấp nhận."));
            return;
        }

        LiveData<NetworkResultWrapper<VolunteerMission>> source = acceptVolunteerMissionUseCase.execute(
                currentDispatchContext.missionId,
                currentDispatchContext.dispatchAttemptId,
                null,
                null
        );
        acceptResult.addSource(source, result -> {
            acceptResult.setValue(enrichResult(result));
            if (result.isLoading()) {
                return;
            }

            acceptResult.removeSource(source);
            if (result.isSuccess()) {
                VolunteerMission mission = enrichMission(result.getData());
                pendingMission.setValue(null);
                currentDispatchContext = null;
                isMissionIgnored.setValue(false);
                isMissionAccepted.setValue(true);
                currentMissionType.setValue(normalizeMissionType(mission != null ? mission.getMissionType() : null));
            }
        });
    }

    public void rejectPendingMission(String reason, @Nullable String reasonDetail) {
        if (currentDispatchContext == null) {
            rejectResult.setValue(NetworkResultWrapper.error("Không tìm thấy nhiệm vụ điều phối để từ chối."));
            return;
        }

        LiveData<NetworkResultWrapper<Boolean>> source = rejectVolunteerMissionUseCase.execute(
                currentDispatchContext.missionId,
                currentDispatchContext.dispatchAttemptId,
                reason,
                reasonDetail
        );
        rejectResult.addSource(source, result -> {
            rejectResult.setValue(result);
            if (result.isLoading()) {
                return;
            }

            rejectResult.removeSource(source);
            if (result.isSuccess()) {
                clearPendingDispatchState(true);
            }
        });
    }

    public void expirePendingDispatch() {
        clearPendingDispatchState(true);
    }

    public void completeMission() {
        currentMissionType.setValue(null);
        currentDeliveryStep.setValue(1);
        isMissionAccepted.setValue(false);
        isMissionIgnored.setValue(true);
    }

    private void loadPendingMission(String missionId) {
        LiveData<NetworkResultWrapper<VolunteerMission>> source = getVolunteerMissionUseCase.execute(missionId);
        pendingMissionResult.addSource(source, result -> {
            pendingMissionResult.setValue(enrichResult(result));
            if (result.isLoading()) {
                return;
            }

            pendingMissionResult.removeSource(source);
            if (result.isSuccess()) {
                pendingMission.setValue(enrichMission(result.getData()));
            }
        });
    }

    private NetworkResultWrapper<VolunteerMission> enrichResult(NetworkResultWrapper<VolunteerMission> result) {
        if (result == null) {
            return null;
        }
        if (result.isSuccess()) {
            return NetworkResultWrapper.success(enrichMission(result.getData()));
        }
        if (result.isError()) {
            return NetworkResultWrapper.error(result.getMessage());
        }
        return NetworkResultWrapper.loading();
    }

    @Nullable
    private VolunteerMission enrichMission(@Nullable VolunteerMission mission) {
        if (mission == null || currentDispatchContext == null) {
            return mission;
        }
        return mission.withDispatchContext(
                currentDispatchContext.dispatchAttemptId,
                currentDispatchContext.expiresAt
        );
    }

    private void clearPendingDispatchState(boolean ignored) {
        pendingMission.setValue(null);
        currentDispatchContext = null;
        currentMissionType.setValue(null);
        currentDeliveryStep.setValue(1);
        isMissionAccepted.setValue(false);
        isMissionIgnored.setValue(ignored);
    }

    @Nullable
    private Instant parseInstant(@Nullable String value) {
        String normalized = normalizeNullable(value);
        if (normalized == null) {
            return null;
        }
        try {
            return Instant.parse(normalized);
        } catch (Exception ignored) {
            return null;
        }
    }

    private String normalize(String value) {
        String normalized = normalizeNullable(value);
        return normalized != null ? normalized : "";
    }

    @Nullable
    private String normalizeNullable(@Nullable String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeMissionType(@Nullable String missionType) {
        String normalized = normalizeNullable(missionType);
        if (normalized == null) {
            return null;
        }
        if ("SUPPLY".equalsIgnoreCase(normalized)) {
            return "DELIVERY";
        }
        return normalized.toUpperCase();
    }

    private static final class DispatchContext {
        final String missionId;
        final String dispatchAttemptId;
        @Nullable
        final String missionType;
        @Nullable
        final Instant expiresAt;

        private DispatchContext(String missionId,
                                String dispatchAttemptId,
                                @Nullable String missionType,
                                @Nullable Instant expiresAt) {
            this.missionId = missionId;
            this.dispatchAttemptId = dispatchAttemptId;
            this.missionType = missionType;
            this.expiresAt = expiresAt;
        }
    }
}
