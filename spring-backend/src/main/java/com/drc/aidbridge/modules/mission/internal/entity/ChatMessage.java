package com.drc.aidbridge.modules.mission.internal.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "chat_messages")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "mission_id", nullable = false)
    private UUID missionId;

    @Column(name = "sender_id", nullable = false)
    private UUID senderId;

    @Column(name = "message_type", nullable = false)
    private String messageType;

    @Column(name = "message_text", columnDefinition = "text")
    private String messageText;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private boolean isRead = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
