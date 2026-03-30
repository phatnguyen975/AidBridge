package com.drc.aidbridge.modules.notification.internal;

import com.drc.aidbridge.modules.notification.NotificationFacade;
import com.drc.aidbridge.modules.notification.internal.service.EmailService;
import com.drc.aidbridge.modules.notification.internal.service.FCMService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationFacadeImpl implements NotificationFacade {

    private final EmailService emailService;
    private final Optional<FCMService> fcmService;

    @Override
    public void sendEmail(String to, String otp) {
        emailService.sendOtpEmail(to, otp);
    }

    @Override
    public void sendPasswordResetEmail(String to, String otp) {
        emailService.sendPasswordResetEmail(to, otp);
    }

    @Override
    public void sendWelcomeEmail(String to, String userName) {
        emailService.sendWelcomeEmail(to, userName);
    }

    @Override
    public void notifyMissionPickupConfirmed(UUID missionId, String volunteerName) {
        fcmService.ifPresentOrElse(service -> {
            FCMService.MissionNotification notification =
                    service.createPickupConfirmedNotification(missionId, volunteerName);
            log.info("Prepared pickup notification for mission {} with type {}", missionId, notification.getType());
        }, () -> log.warn("FCM service unavailable, skipping pickup notification for mission {}", missionId));
    }

    @Override
    public void notifyMissionCompleted(UUID missionId, String volunteerName) {
        fcmService.ifPresentOrElse(service -> {
            FCMService.MissionNotification notification =
                    service.createMissionCompletedNotification(missionId, volunteerName);
            log.info("Prepared completion notification for mission {} with type {}", missionId, notification.getType());
        }, () -> log.warn("FCM service unavailable, skipping completion notification for mission {}", missionId));
    }

    @Override
    public void notifyMissionCancelled(UUID missionId, String reason) {
        fcmService.ifPresentOrElse(service -> {
            FCMService.MissionNotification notification =
                service.createMissionCancelledNotification(missionId, reason);
            log.info("Prepared cancellation notification for mission {} with type {}", missionId,
                notification.getType());
        }, () -> log.warn("FCM service unavailable, skipping cancellation notification for mission {}", missionId));
    }
}
