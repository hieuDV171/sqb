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

import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Base64;
import java.util.Optional;

@Slf4j
@Configuration
public class FirebaseConfig {

    @Value("${app.firebase.config-path:classpath:firebase-service-account.json}")
    private Resource firebaseConfigResource;

    @Value("${app.firebase.credentials-base64:}")
    private String credentialsBase64;

    @Bean
    public FirebaseApp firebaseApp() {
        try {
            if (!FirebaseApp.getApps().isEmpty()) {
                return FirebaseApp.getInstance();
            }

            // 1. Ưu tiên nạp từ chuỗi Base64 truyền qua Environment Variable (Chuẩn Docker / VPS)
            if (StringUtils.hasText(credentialsBase64)) {
                byte[] decodedBytes = Base64.getDecoder().decode(credentialsBase64.trim());
                try (InputStream serviceAccount = new ByteArrayInputStream(decodedBytes)) {
                    FirebaseOptions options = FirebaseOptions.builder()
                            .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                            .build();

                    FirebaseApp app = FirebaseApp.initializeApp(options);
                    log.info("[FirebaseConfig] Khởi tạo Firebase Admin SDK thành công từ biến môi trường FIREBASE_CREDENTIALS_BASE64!");
                    return app;
                }
            }

            // 2. Fallback nạp từ file cấu hình (classpath hoặc file: path mount từ volume)
            if (firebaseConfigResource != null && firebaseConfigResource.exists()) {
                try (InputStream serviceAccount = firebaseConfigResource.getInputStream()) {
                    FirebaseOptions options = FirebaseOptions.builder()
                            .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                            .build();

                    FirebaseApp app = FirebaseApp.initializeApp(options);
                    log.info("[FirebaseConfig] Khởi tạo Firebase Admin SDK thành công từ file cấu hình: {}", firebaseConfigResource.getDescription());
                    return app;
                }
            } else {
                log.warn("[FirebaseConfig] Không tìm thấy cấu hình Firebase (cả biến môi trường lẫn file). Tính năng Push Notification sẽ tạm thời không hoạt động.");
            }
        } catch (Exception e) {
            log.error("[FirebaseConfig] Lỗi khi khởi tạo Firebase Admin SDK: {}", e.getMessage());
        }
        return null;
    }

    @Bean
    public FirebaseMessaging firebaseMessaging(Optional<FirebaseApp> firebaseApp) {
        return firebaseApp.map(FirebaseMessaging::getInstance).orElse(null);
    }

}
