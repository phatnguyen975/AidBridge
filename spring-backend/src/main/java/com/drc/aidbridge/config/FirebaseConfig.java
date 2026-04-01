package com.drc.aidbridge.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Firebase Configuration for FCM (Firebase Cloud Messaging).
 *
 * Setup steps:
 * 1. Download service account key JSON from Firebase Console
 * 2. Place file in src/main/resources/ (e.g., firebase-service-account.json)
 * 3. Update application.yaml with correct path
 * 4. Make sure to add JSON file to .gitignore for security
 */
@Slf4j
@Configuration
@ConditionalOnProperty(value = "firebase.enabled", havingValue = "true")
public class FirebaseConfig {

    @Value("${firebase.service-account.path:firebase-service-account.json}")
    private String serviceAccountPath;

    @Value("${firebase.project-id:aidbridge-5613d}")
    private String projectId;

    @Bean
    @ConditionalOnMissingBean(FirebaseMessaging.class)
    public FirebaseMessaging firebaseMessaging() throws IOException {
        // Check if FirebaseApp already initialized
        if (FirebaseApp.getApps().isEmpty()) {
            log.info("Initializing Firebase connection...");

            // Load service account key
            InputStream serviceAccount;
            try {
                // Try classpath first
                serviceAccount = new ClassPathResource(serviceAccountPath).getInputStream();
                log.info("Loading Firebase service account from classpath: {}", serviceAccountPath);
            } catch (Exception e) {
                // Fallback to file system
                serviceAccount = new FileInputStream(serviceAccountPath);
                log.info("Loading Firebase service account from filesystem: {}", serviceAccountPath);
            }

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .setProjectId(projectId)
                    .build();

            FirebaseApp.initializeApp(options);
            log.info("Firebase initialized successfully for project: {}", projectId);
        } else {
            log.info("Firebase already initialized");
        }

        return FirebaseMessaging.getInstance();
    }
}
