package com.drc.aidbridge.modules.notification.internal.service;

import com.google.firebase.messaging.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

/**
 * FCM Notification Service for push notifications.
 *
 * Handles all FCM notifications as defined in data-architecture.md:
 * - High priority: Dispatch requests, SOS assignments, volunteer arrivals
 * - Normal priority: Mission completion, ratings, inventory alerts
 *
 * Notification channels (Android):
 * - emergency: High priority, custom sound, long vibration
 * - updates: Default priority, default sound, short vibration
 * - chat: Default priority, message sound, short vibration
 * - system: Low priority, no sound, no vibration
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FCMService {

    private final FirebaseMessaging firebaseMessaging;

    /**
     * Send high priority notification for mission-related events.
     */
    public void sendMissionNotification(String fcmToken, MissionNotification notification) {
        if (fcmToken == null || fcmToken.isEmpty()) {
            log.warn("No FCM token available for mission notification: {}", notification.getType());
            return;
        }

        try {
            Message message = Message.builder()
                    .setToken(fcmToken)
                    .setNotification(Notification.builder()
                            .setTitle(notification.getTitle())
                            .setBody(notification.getBody())
                            .build())
                    .putAllData(notification.getData())
                    .setAndroidConfig(AndroidConfig.builder()
                            .setPriority(notification.getPriority().toFirebasePriority())
                            .setNotification(AndroidNotification.builder()
                                    .setChannelId(notification.getChannelId())
                                    .setSound(notification.getSound())
                                    .build())
                            .build())
                    .setApnsConfig(ApnsConfig.builder()
                            .setAps(Aps.builder()
                                    .setSound(notification.getSound())
                                    .setBadge(1)
                                    .build())
                            .build())
                    .build();

            String response = firebaseMessaging.send(message);

            log.info("FCM notification sent successfully: {} to token: {} - Response ID: {}",
                    notification.getType(), fcmToken.substring(0, Math.min(10, fcmToken.length())) + "...", response);

        } catch (FirebaseMessagingException e) {
            if (e.getMessagingErrorCode() == MessagingErrorCode.INVALID_ARGUMENT) {
                log.error("Invalid FCM token: {}", fcmToken.substring(0, Math.min(10, fcmToken.length())) + "...");
            } else if (e.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED) {
                log.warn("FCM token unregistered: {}", fcmToken.substring(0, Math.min(10, fcmToken.length())) + "...");
            } else {
                log.error("Failed to send FCM notification: {} to token: {}",
                        notification.getType(), fcmToken.substring(0, Math.min(10, fcmToken.length())) + "...", e);
            }
        } catch (Exception e) {
            log.error("Unexpected error sending FCM notification: {} to token: {}",
                    notification.getType(), fcmToken.substring(0, Math.min(10, fcmToken.length())) + "...", e);
        }
    }

    /**
     * Create notification for pickup confirmation.
     */
    public MissionNotification createPickupConfirmedNotification(UUID missionId, String volunteerName) {
        return MissionNotification.builder()
                .type(NotificationType.MISSION_PICKUP)
                .title("📦 Vật phẩm đã được lấy")
                .body(volunteerName + " đã lấy vật phẩm và đang đến")
                .channelId("updates")
                .priority(AndroidPriority.HIGH)
                .sound("default")
                .data(Map.of(
                        "type", "MISSION_UPDATE",
                        "mission_id", missionId.toString(),
                        "click_action", "OPEN_MISSION_DETAIL"))
                .build();
    }

    /**
     * Create notification for mission completion.
     */
    public MissionNotification createMissionCompletedNotification(UUID missionId, String volunteerName) {
        return MissionNotification.builder()
                .type(NotificationType.MISSION_COMPLETED)
                .title("✅ Đã nhận được hỗ trợ")
                .body("Cảm ơn bạn! " + volunteerName + " đã hoàn thành hỗ trợ")
                .channelId("updates")
                .priority(AndroidPriority.HIGH)
                .sound("default")
                .data(Map.of(
                        "type", "MISSION_COMPLETED",
                        "mission_id", missionId.toString(),
                        "click_action", "OPEN_RATING"))
                .build();
    }

    /**
     * Create notification for mission cancellation.
     */
    public MissionNotification createMissionCancelledNotification(UUID missionId, String reason) {
        return MissionNotification.builder()
                .type(NotificationType.MISSION_CANCELLED)
                .title("❌ Hỗ trợ bị hủy")
                .body("Lý do: " + reason + ". Chúng tôi sẽ tìm tình nguyện viên khác")
                .channelId("updates")
                .priority(AndroidPriority.HIGH)
                .sound("default")
                .data(Map.of(
                        "type", "MISSION_CANCELLED",
                        "mission_id", missionId.toString(),
                        "click_action", "OPEN_SOS_STATUS"))
                .build();
    }

    /**
     * Create notification for volunteer assignment.
     */
    public MissionNotification createVolunteerAssignedNotification(UUID missionId, String volunteerName,
            int etaMinutes) {
        return MissionNotification.builder()
                .type(NotificationType.VOLUNTEER_ASSIGNED)
                .title("🆘 Đã tìm được người hỗ trợ")
                .body(volunteerName + " đang đến, dự kiến " + etaMinutes + " phút")
                .channelId("emergency")
                .priority(AndroidPriority.HIGH)
                .sound("emergency.wav")
                .data(Map.of(
                        "type", "VOLUNTEER_ASSIGNED",
                        "mission_id", missionId.toString(),
                        "click_action", "OPEN_TRACKING"))
                .build();
    }

    /**
     * Create notification when volunteer arrives.
     */
    public MissionNotification createVolunteerArrivedNotification(UUID missionId, String volunteerName) {
        return MissionNotification.builder()
                .type(NotificationType.VOLUNTEER_ARRIVED)
                .title("📍 Tình nguyện viên đã đến")
                .body(volunteerName + " đang ở vị trí của bạn")
                .channelId("emergency")
                .priority(AndroidPriority.HIGH)
                .sound("emergency.wav")
                .data(Map.of(
                        "type", "VOLUNTEER_ARRIVED",
                        "mission_id", missionId.toString(),
                        "click_action", "OPEN_CHAT"))
                .build();
    }

    // ==================== Data Classes ====================

    @Data
    @lombok.Builder
    public static class MissionNotification {
        private NotificationType type;
        private String title;
        private String body;
        private String channelId;
        private AndroidPriority priority;
        private String sound;
        private Map<String, String> data;
    }

    public enum NotificationType {
        MISSION_PICKUP,
        MISSION_COMPLETED,
        MISSION_CANCELLED,
        VOLUNTEER_ASSIGNED,
        VOLUNTEER_ARRIVED,
        DISPATCH_REQUEST
    }

    public enum AndroidPriority {
        HIGH("high"),
        NORMAL("normal");

        private final String value;

        AndroidPriority(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        /**
         * Convert to Firebase AndroidConfig.Priority.
         */
        public AndroidConfig.Priority toFirebasePriority() {
            return this == HIGH ? AndroidConfig.Priority.HIGH : AndroidConfig.Priority.NORMAL;
        }
    }
}
