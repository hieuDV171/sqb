package com.frozenheart.backend.core.config;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import com.frozenheart.backend.core.constant.Time;
import com.frozenheart.backend.core.entity.badge.*;
import com.frozenheart.backend.core.entity.user.*;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.frozenheart.backend.core.entity.badge.criteria.CounterThresholdCriteria;
import com.frozenheart.backend.core.entity.badge.criteria.LeaderboardRankCriteria;
import com.frozenheart.backend.core.entity.badge.criteria.StreakCriteria;
import com.frozenheart.backend.modules.badge.repository.BadgeRepository;
import com.frozenheart.backend.modules.gamification.dto.LeaderboardPeriod;
import com.frozenheart.backend.modules.gamification.repository.UserGamificationRepository;
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
    private final UserGamificationRepository userGamificationRepository;

    private final UserPushSettingRepository userPushSettingRepository;
    private final PasswordEncoder passwordEncoder;

    private final BadgeRepository badgeRepository;

    @Value("${spring.mail.username}")
    private String adminEmail;

    @Override
    @Transactional
    public void run(String @NonNull ... args) {

        if (adminEmail != null && !adminEmail.isBlank() && !userRepository.existsByEmail(adminEmail)) {
            log.info("Khởi tạo tài khoản ADMIN mặc định: {}", adminEmail);

            Instant now = Instant.now();
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
                    .schoolFaculty("")
                    .major("Quản trị hệ thống")
                    .timezone(Time.DEFAULT_TIMEZONE)
                    .avatarUrl("")
                    .coverUrl("")
                    .avatarFrameUrl("")
                    .gender(Gender.MALE)
                    .dateOfBirth(LocalDate.of(2003, 1, 17))
                    .bio("Admin tối cao của SQB HUST")
                    .totalProposedQuestion(0)
                    .totalApprovedQuestions(0)
                    .badgesCount(0)
                    .friendsCount(0)
                    .followersCount(0)
                    .followingCount(0)
                    .profileCompleted(true)
                    .build();

            userProfileRepository.save(adminProfile);

            UserGamification adminGamification = UserGamification.builder()
                    .user(savedAdmin)
                    .publicPoints(0.0)
                    .secretPoints(0.0)
                    .coinBalance(0.0)
                    .currentStreak(0)
                    .build();
            userGamificationRepository.save(adminGamification);

            UserPushSetting pushSetting = UserPushSetting.builder()
                    .user(savedAdmin)
                    .preferences(createDefaultPushPreferences())
                    .updatedAt(Instant.now())
                    .build();
            userPushSettingRepository.save(pushSetting);
            log.info("Tạo tài khoản ADMIN thành công!");

        }

        initDefaultBadges();
    }

    private void initDefaultBadges() {
        if (badgeRepository.count() > 0) {
            return;
        }
        log.info("Khởi tạo bộ huy hiệu mặc định cho hệ thống...");

        Instant now = Instant.now();
        List<Badge> defaultBadges = List.of(
                Badge.builder()
                        .name("Tập sự Biên soạn")
                        .description("Được phê duyệt 10 câu hỏi vào Ngân hàng đề thi")
                        .badgeTier(BadgeTier.BRONZE)
                        .badgeTriggerEvent(BadgeTriggerEvent.APPROVED_QUESTIONS)
                        .iconUrl("/badges/junior_contributor.png")
                        .criteria(CounterThresholdCriteria.builder()
                                .threshold(10)
                                .targetRole(BadgeTargetRole.STUDENT)
                                .build())
                        .createdAt(now)
                        .build(),
                Badge.builder()
                        .name("Cây bút Vàng")
                        .description("Được phê duyệt 50 câu hỏi vào Ngân hàng đề thi")
                        .badgeTier(BadgeTier.SILVER)
                        .badgeTriggerEvent(BadgeTriggerEvent.APPROVED_QUESTIONS)
                        .iconUrl("/badges/senior_author.png")
                        .criteria(CounterThresholdCriteria.builder()
                                .threshold(50)
                                .targetRole(BadgeTargetRole.STUDENT)
                                .build())
                        .createdAt(now)
                        .build(),
                Badge.builder()
                        .name("Kho tàng Tri thức")
                        .description("Được phê duyệt 200 câu hỏi vào Ngân hàng đề thi")
                        .badgeTier(BadgeTier.GOLD)
                        .badgeTriggerEvent(BadgeTriggerEvent.APPROVED_QUESTIONS)
                        .iconUrl("/badges/grandmaster_questioner.png")
                        .criteria(CounterThresholdCriteria.builder()
                                .threshold(200)
                                .targetRole(BadgeTargetRole.STUDENT)
                                .build())
                        .createdAt(now)
                        .build(),
                Badge.builder()
                        .name("Người gác cổng Tri thức")
                        .description("Thẩm định và phê duyệt 50 phiên đề xuất câu hỏi")
                        .badgeTier(BadgeTier.GOLD)
                        .badgeTriggerEvent(BadgeTriggerEvent.REVIEWED_SESSIONS)
                        .iconUrl("/badges/guardian_reviewer.png")
                        .criteria(CounterThresholdCriteria.builder()
                                .threshold(50)
                                .targetRole(BadgeTargetRole.LECTURER)
                                .build())
                        .createdAt(now)
                        .build(),
                Badge.builder()
                        .name("Sứ giả Kết nối")
                        .description("Kết nối thành công với 20 người bạn trong trường")
                        .badgeTier(BadgeTier.BRONZE)
                        .badgeTriggerEvent(BadgeTriggerEvent.ACCEPTED_FRIENDS)
                        .iconUrl("/badges/campus_connector.png")
                        .criteria(CounterThresholdCriteria.builder()
                                .threshold(20)
                                .targetRole(BadgeTargetRole.ALL)
                                .build())
                        .createdAt(now)
                        .build(),
                Badge.builder()
                        .name("Ngọn lửa Tri thức")
                        .description("Duy trì chuỗi học tập và hoạt động liên tiếp 7 ngày")
                        .badgeTier(BadgeTier.SILVER)
                        .badgeTriggerEvent(BadgeTriggerEvent.STUDY_STREAK)
                        .iconUrl("/badges/study_streak.png")
                        .criteria(StreakCriteria.builder()
                                .streakDays(7)
                                .build())
                        .createdAt(now)
                        .build(),
                Badge.builder()
                        .name("Quán quân Bảng Vàng")
                        .description("Đạt Top 1 Bảng xếp hạng điểm cống hiến học kỳ")
                        .badgeTier(BadgeTier.PLATINUM)
                        .badgeTriggerEvent(BadgeTriggerEvent.LEADERBOARD_RANK)
                        .iconUrl("/badges/leaderboard_champion.png")
                        .criteria(LeaderboardRankCriteria.builder()
                                .targetRank(1)
                                .period(LeaderboardPeriod.SEMESTER)
                                .build())
                        .createdAt(now)
                        .build());

        badgeRepository.saveAll(defaultBadges);
        log.info("Khởi tạo thành công {} huy hiệu mặc định!", defaultBadges.size());
    }

    private PushPreferences createDefaultPushPreferences() {
        return PushPreferences.createDefault();
    }

}
