package com.drc.aidbridge.modules.notification;

import java.util.UUID;

public interface NotificationFacade {

    void sendEmail(String to, String otp);

    void sendPasswordResetEmail(String to, String otp);

    void sendWelcomeEmail(String to, String userName);

    void notifyMissionPickupConfirmed(UUID missionId, String volunteerName);

    void notifyMissionCompleted(UUID missionId, String volunteerName);

    void notifyMissionCancelled(UUID missionId, String reason);
}
