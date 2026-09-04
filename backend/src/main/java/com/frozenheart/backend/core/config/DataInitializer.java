package com.frozenheart.backend.core.config;

import java.time.LocalDateTime;

import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.frozenheart.backend.core.entity.user.GamificationPointsJson;
import com.frozenheart.backend.core.entity.user.PushPreferences;
import com.frozenheart.backend.core.entity.user.User;
import com.frozenheart.backend.core.entity.user.UserProfile;
import com.frozenheart.backend.core.entity.user.UserPushSetting;
import com.frozenheart.backend.core.entity.user.UserRole;
import com.frozenheart.backend.modules.user.repository.UserProfileRepository;
import com.frozenheart.backend.modules.user.repository.UserPushSettingRepository;
import com.frozenheart.backend.modules.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;

    private final UserPushSettingRepository userPushSettingRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${spring.mail.username}")
    private String adminEmail;

    @Override
    @Transactional
    public void run(String @NonNull... args) {

        if (adminEmail != null && !adminEmail.isBlank() && !userRepository.existsByEmail(adminEmail)) {
            log.info("Khởi tạo tài khoản ADMIN mặc định: {}", adminEmail);

            LocalDateTime now = LocalDateTime.now();
            // Tạo User Admin (Password: Admin@123456)
            User adminUser = User.builder()
                    .email(adminEmail)
                    .passwordHash(passwordEncoder.encode("Admin@123456")) // Băm mật khẩu BCrypt
                    .role(UserRole.ADMIN)
                    .verified(true)
                    .active(true)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();

            User savedAdmin = userRepository.save(adminUser);

            // Tạo UserProfile Admin
            UserProfile adminProfile = UserProfile.builder()
                    .user(savedAdmin)
                    .fullName("Quản Trị Viên Hệ Thống")
                    .studentLecturerCode("ADMIN001")
                    .faculty("")
                    .major("Quản trị hệ thống")
                    .avatarUrl("")
                    .coverUrl("")
                    .avatarFrameUrl("")
                    .bio("Admin tối cao của SQB HUST")
                    .totalProposedQuestion(0)
                    .totalApprovedQuestions(0)
                    .gamificationPoints(new GamificationPointsJson(0.0, 0.0))
                    .badgesCount(0)
                    .friendsCount(0)
                    .followersCount(0)
                    .followingCount(0)
                    .profileCompleted(true)
                    .build();

            userProfileRepository.save(adminProfile);

            UserPushSetting pushSetting = UserPushSetting.builder()
                    .user(savedAdmin)
                    .preferences(createDefaultPushPreferences())
                    .updatedAt(LocalDateTime.now())
                    .build();
            userPushSettingRepository.save(pushSetting);
            log.info("Tạo tài khoản ADMIN thành công!");

        }
    }

    private PushPreferences createDefaultPushPreferences() {
        return PushPreferences.createDefault();
    }

}
