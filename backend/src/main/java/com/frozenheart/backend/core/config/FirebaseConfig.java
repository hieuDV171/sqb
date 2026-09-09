package com.frozenheart.backend.core.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.io.InputStream;

@Slf4j
@Configuration
public class FirebaseConfig {

    @Value("${app.firebase.config-path:classpath:firebase-service-account.json}")
    private Resource firebaseConfigResource;

    @Bean
    public FirebaseApp firebaseApp() {
        try {
            if (!FirebaseApp.getApps().isEmpty()) {
                return FirebaseApp.getInstance();
            }

            if (firebaseConfigResource != null && firebaseConfigResource.exists()) {
                try (InputStream serviceAccount = firebaseConfigResource.getInputStream()) {
                    FirebaseOptions options = FirebaseOptions.builder()
                            .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                            .build();

                    FirebaseApp app = FirebaseApp.initializeApp(options);
                    log.info("[FirebaseConfig] Khởi tạo Firebase Admin SDK thành công!");
                    return app;
                }
            } else {
                log.warn("[FirebaseConfig] Không tìm thấy file cấu hình Firebase. Tính năng Push Notification sẽ tạm thời không hoạt động.");
            }
        } catch (Exception e) {
            log.error("[FirebaseConfig] Lỗi khi khởi tạo Firebase Admin SDK: {}", e.getMessage());
        }
        return null;
    }

    @Bean
    public FirebaseMessaging firebaseMessaging(FirebaseApp firebaseApp) {
        if (firebaseApp == null) {
            return null;
        }
        return FirebaseMessaging.getInstance(firebaseApp);
    }
}
