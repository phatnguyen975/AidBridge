package com.drc.aidbridge.modules.notification;

import com.drc.aidbridge.modules.shared.enums.DispatchType;
import com.drc.aidbridge.modules.shared.enums.MissionType;

import java.time.Instant;
import java.util.UUID;

public interface NotificationFacade {

    void sendEmail(String to, String otp);

    void sendPasswordResetEmail(String to, String otp);

    void sendWelcomeEmail(String to, String userName);

    void notifyMissionPickupConfirmed(UUID missionId, String volunteerName);

    void notifyMissionCompleted(UUID missionId, String volunteerName);

    void notifyMissionCancelled(UUID missionId, String reason);

    void notifyDispatchRequest(UUID volunteerId, UUID missionId, UUID dispatchAttemptId,
                               Instant expiresAt, MissionType missionType, DispatchType dispatchType);
}
