package com.drc.aidbridge.modules.notification.internal.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

@Slf4j
@Configuration
@ConditionalOnProperty(value = "firebase.enabled", havingValue = "true")
public class FirebaseConfig {

    @Value("${firebase.service-account.path:firebase-service-account.json}")
    private String serviceAccountPath;

    @Value("${firebase.project-id:aidbridge-5613d}")
    private String projectId;

    @Bean
    public FirebaseMessaging firebaseMessaging() throws IOException {
        if (FirebaseApp.getApps().isEmpty()) {
            log.info("Initializing Firebase connection...");

            InputStream serviceAccount;
            try {
                serviceAccount = new ClassPathResource(serviceAccountPath).getInputStream();
                log.info("Loading Firebase service account from classpath: {}", serviceAccountPath);
            } catch (Exception e) {
                serviceAccount = new FileInputStream(serviceAccountPath);
                log.info("Loading Firebase service account from filesystem: {}", serviceAccountPath);
            }

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .setProjectId(projectId)
                    .build();

            FirebaseApp.initializeApp(options);
            log.info("Firebase initialized successfully for project: {}", projectId);
        }

        return FirebaseMessaging.getInstance();
    }
}
