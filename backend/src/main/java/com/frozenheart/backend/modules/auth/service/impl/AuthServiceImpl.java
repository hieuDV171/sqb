package com.frozenheart.backend.modules.auth.service.impl;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.frozenheart.backend.modules.auth.dto.admin.AdminResetPasswordRequest;
import com.frozenheart.backend.modules.auth.dto.admin.AuthResponse;
import com.frozenheart.backend.modules.auth.dto.admin.RegisterRequest;
import com.frozenheart.backend.modules.auth.dto.admin.ResendOtpRequest;
import com.frozenheart.backend.modules.auth.dto.admin.VerifyOtpRequest;
import com.frozenheart.backend.modules.auth.dto.admin.AdminBulkCreateUserDto.BulkImportRequest;
import com.frozenheart.backend.modules.auth.dto.admin.AdminBulkCreateUserDto.BulkImportResult;
import com.frozenheart.backend.modules.auth.dto.admin.AdminBulkCreateUserDto.SingleUserImportDto;
import com.frozenheart.backend.modules.auth.dto.user.ChangePasswordRequest;
import com.frozenheart.backend.modules.auth.dto.user.LoginRequest;
import com.frozenheart.backend.modules.auth.dto.user.RefreshTokenRequest;
import com.frozenheart.backend.modules.auth.dto.user.RefreshTokenResponse;
import com.frozenheart.backend.modules.auth.service.AuthService;
import com.frozenheart.backend.modules.auth.service.EmailService;
import com.frozenheart.backend.modules.device.repository.UserDeviceRepository;
import com.frozenheart.backend.modules.user.repository.UserProfileRepository;
import com.frozenheart.backend.modules.user.repository.UserPushSettingRepository;
import com.frozenheart.backend.modules.user.repository.UserRepository;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.frozenheart.backend.core.constant.ResponseCode;
import com.frozenheart.backend.core.constant.Time;
import com.frozenheart.backend.core.dto.event.EntitySearchSyncEvent;
import com.frozenheart.backend.core.dto.jwt.JwtPayload;
import com.frozenheart.backend.core.entity.user.DevicePlatform;
import com.frozenheart.backend.core.entity.user.PushPreferences;
import com.frozenheart.backend.core.entity.user.UserGamification;
import com.frozenheart.backend.modules.gamification.repository.UserGamificationRepository;
import com.frozenheart.backend.core.entity.user.User;
import com.frozenheart.backend.core.entity.user.UserDevice;
import com.frozenheart.backend.core.entity.user.UserProfile;
import com.frozenheart.backend.core.entity.user.UserPushSetting;
import com.frozenheart.backend.core.entity.user.UserRole;
import com.frozenheart.backend.core.exception.AppException;
import com.frozenheart.backend.core.security.JwtService;
import com.frozenheart.backend.core.util.CookieUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final CookieUtils cookieUtils;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserGamificationRepository userGamificationRepository;
    private final UserPushSettingRepository userPushSettingRepository;
    private final UserDeviceRepository userDeviceRepository;
    private final PasswordEncoder passwordEncoder;
    private final RedisTemplate<String, String> redisTemplate;
    private final JwtService jwtService;
    private final EmailService emailService;

    private final ApplicationEventPublisher eventPublisher;

    private static final String OTP_PREFIX = "otp:";
    private static final String RESEND_LOCK_PREFIX = "resend_lock:";

    @Transactional
    public AuthResponse login(LoginRequest request) {
        String email = request.email();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ResponseCode.USER_NOT_FOUND));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            log.error("[AuthServiceImpl]: {}", ResponseCode.INCORRECT_IDENTIFIER);
            throw new AppException(ResponseCode.INCORRECT_IDENTIFIER);
        }

        Long userId = user.getId();

        UserProfile userProfile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new AppException(ResponseCode.USER_NOT_FOUND));

        if (!user.isVerified()) {
            throw new AppException(ResponseCode.USER_NOT_VERIFIED);
        }

        if (!user.isActive()) {
            throw new AppException(ResponseCode.ACCOUNT_NOT_ACTIVE);
        }

        saveOrUpdateUserDevice(user, request);

        AuthResponse authResponse = buildAuthResponse(request.deviceId(), user, userProfile);

        if (request.platform() == DevicePlatform.WEB) {
            cookieUtils.addHttpOnlyCookie(
                    "refreshToken",
                    authResponse.refreshToken(),
                    Duration.ofMillis(jwtService.getRefreshTokenExpiration()));

            return AuthResponse.builder()
                    .id(authResponse.id())
                    .username(authResponse.username())
                    .accessToken(authResponse.accessToken())
                    .refreshToken(null) // WEB không nhận refreshToken trong JSON Body
                    .avatarUrl(authResponse.avatarUrl())
                    .coverUrl(authResponse.coverUrl())
                    .frameUrl(authResponse.frameUrl())
                    .verified(authResponse.verified())
                    .profileCompleted(authResponse.profileCompleted())
                    .build();
        }

        return authResponse;

    }

    private void saveOrUpdateUserDevice(User user, LoginRequest request) {
        // 1. Device takeover: Thu hồi phiên của bất kỳ tài khoản nào khác đang active
        // trên thiết bị này
        List<UserDevice> otherActiveDevices = userDeviceRepository
                .findActiveDevicesByDeviceIdWithUser(request.deviceId());
        List<UserDevice> devicesToDeactivate = new ArrayList<>();
        for (UserDevice otherDevice : otherActiveDevices) {
            if (otherDevice.getUser() != null && !otherDevice.getUser().getId().equals(user.getId())) {
                otherDevice.setActive(false);
                otherDevice.setFcmToken(null);
                devicesToDeactivate.add(otherDevice);

                // Xóa Refresh Token của người dùng cũ trong Redis
                String oldEmail = otherDevice.getUser().getEmail();
                if (oldEmail != null) {
                    redisTemplate.delete("rt:" + oldEmail + ":" + request.deviceId());
                    log.info("[AuthService] Đã tự động thu hồi phiên của user {} trên thiết bị {}", oldEmail,
                            request.deviceId());
                }
            }
        }
        if (!devicesToDeactivate.isEmpty()) {
            userDeviceRepository.saveAll(devicesToDeactivate);
        }

        // 2. Kích hoạt hoặc tạo mới UserDevice cho người dùng hiện tại
        UserDevice device = userDeviceRepository.findByUserIdAndDeviceId(user.getId(), request.deviceId())
                .orElseGet(() -> UserDevice.builder()
                        .user(user)
                        .deviceId(request.deviceId())
                        .createdAt(Instant.now())
                        .build());

        device.setFcmToken(request.fcmToken());
        device.setPlatform(request.platform());
        device.setDeviceName(request.deviceName());
        device.setOsVersion(request.osVersion());
        device.setAppVersion(request.appVersion());
        device.setActive(true);
        device.setLastActiveAt(Instant.now());

        userDeviceRepository.save(device);
    }

    @Transactional
    @Override
    public void register(RegisterRequest request) {
        // kiểm tra email tồn tại
        String email = request.email();

        if (userRepository.existsByEmail(email)) {
            throw new AppException(ResponseCode.USER_ALREADY_EXISTS);
        }

        UserRole role = UserRole.valueOf(request.role().name());

        Instant now = Instant.now();

        // Tạo tài khoản (chưa xác thực OTP)
        User user = User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(role)
                .verified(false)
                .active(true)
                .createdAt(now)
                .updatedAt(now)
                .build();

        User savedUser = userRepository.save(user);

        String userTz = Time.DEFAULT_TIMEZONE;
        if (request.timezone() != null && !request.timezone().isBlank()) {
            try {
                userTz = ZoneId.of(request.timezone().trim()).getId();
            } catch (Exception e) {
                log.warn("Invalid timezone '{}' in register request, falling back to {}", request.timezone(), Time.DEFAULT_TIMEZONE);
            }
        }

        UserProfile userProfile = UserProfile.builder()
                .user(savedUser)
                .fullName("Sóc bay đỏ mận" + email.substring(0, email.indexOf('@')))
                .avatarUrl("")
                .coverUrl("")
                .avatarFrameUrl("")
                .bio("Nhà tiên tri siu cấp zũ trụ!")
                .faculty("")
                .major("")
                .studentLecturerCode("")
                .timezone(userTz)
                .totalProposedQuestion(0)
                .totalApprovedQuestions(0)
                .badgesCount(0)
                .friendsCount(0)
                .followersCount(0)
                .followingCount(0)
                .profileCompleted(false)
                .build();

        userProfileRepository.save(userProfile);

        UserGamification userGamification = UserGamification.builder()
                .user(savedUser)
                .publicPoints(0.0)
                .secretPoints(0.0)
                .coinBalance(0.0)
                .currentStreak(0)
                .build();
        userGamificationRepository.save(userGamification);

        PushPreferences pushPreferences = createDefaultPushPreferences();

        UserPushSetting userPushSetting = UserPushSetting.builder()
                .user(savedUser)
                .preferences(pushPreferences)
                .updatedAt(Instant.now())
                .build();

        userPushSettingRepository.save(userPushSetting);

        // Sinh mã OTP 6 số và lưu vào Redis với TTL 2 phút
        String otpCode = generateOtpCode();
        redisTemplate.opsForValue().set(OTP_PREFIX + email, otpCode, Duration.ofMinutes(2));

        emailService.sendOtpEmail(email, otpCode);
    }

    @Override
    @Transactional
    public AuthResponse verify(VerifyOtpRequest request) {
        String email = request.email();
        String savedOtp = redisTemplate.opsForValue().get(OTP_PREFIX + email);

        // Kiểm tra mã OTP
        if (savedOtp == null || !savedOtp.equals(request.verifyCode())) {
            throw new AppException(ResponseCode.VERIFICATION_CODE_INVALID);
        }

        // Cập nhật thông tin User thành verified
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ResponseCode.USER_NOT_FOUND));

        user.setVerified(true);
        userRepository.save(user);

        // Xóa OTP trong Redis sau khi đã verify thành công
        redisTemplate.delete(OTP_PREFIX + email);

        String deviceId = request.deviceId();

        Long userId = user.getId();
        UserProfile userProfile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new AppException(ResponseCode.USER_NOT_FOUND));
        eventPublisher.publishEvent(EntitySearchSyncEvent.upsert(EntitySearchSyncEvent.EntityType.USER, user.getId()));

        return buildAuthResponse(deviceId, user, userProfile);

    }

    private Map<String, String> generateAtAndRt(Long userId, UserRole role, String email, String deviceId) {

        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("userId", userId);
        extraClaims.put("role", role);
        extraClaims.put("deviceId", deviceId);

        String accessToken = jwtService.generateAccessToken(extraClaims, email);
        String refreshToken = jwtService.generateRefreshToken(email, deviceId);

        Map<String, String> result = new HashMap<>();
        result.put("accessToken", accessToken);
        result.put("refreshToken", refreshToken);

        return result;
    }

    private AuthResponse buildAuthResponse(String deviceId, User user, UserProfile userProfile) {

        Long userId = user.getId();
        UserRole role = user.getRole();
        String email = user.getEmail();

        Map<String, String> rtAtResult = generateAtAndRt(userId, role, email, deviceId);

        String accessToken = rtAtResult.get("accessToken");
        String refreshToken = rtAtResult.get("refreshToken");

        String redisKey = "rt:" + email + ":" + deviceId;
        redisTemplate.opsForValue().set(
                redisKey,
                refreshToken,
                Duration.ofMillis(jwtService.getRefreshTokenExpiration()));

        return AuthResponse.builder()
                .id(userId)
                .username(user.getEmail())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .avatarUrl(userProfile.getAvatarUrl())
                .coverUrl(userProfile.getCoverUrl())
                .frameUrl(userProfile.getAvatarFrameUrl())
                .verified(user.isVerified())
                .profileCompleted(userProfile.isProfileCompleted())
                .build();
    }

    @Override
    public void resendVerify(ResendOtpRequest request) {
        String email = request.email();
        String lockKey = RESEND_LOCK_PREFIX + email;

        Boolean isLocked = redisTemplate.hasKey(lockKey);

        if (Boolean.TRUE.equals(isLocked)) {
            throw new AppException(ResponseCode.ACTION_ALREADY_PERFORMED);
        }

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new AppException(ResponseCode.USER_NOT_FOUND));

        if (user.isVerified()) {
            throw new AppException(ResponseCode.ACTION_ALREADY_PERFORMED);
        }

        String newOtp = generateOtpCode();
        redisTemplate.opsForValue().set(OTP_PREFIX + email, newOtp, Duration.ofMinutes(2));
        redisTemplate.opsForValue().set(lockKey, "1", Duration.ofSeconds(60));

        emailService.sendOtpEmail(email, newOtp);
    }

    private String generateOtpCode() {
        SecureRandom random = new SecureRandom();
        int code = 100000 + random.nextInt(900000);
        return String.valueOf(code);
    }

    @Override
    @Transactional
    public BulkImportResult bulkImportUsers(BulkImportRequest request) {
        if (request == null || request.users() == null || request.users().isEmpty()) {
            return new BulkImportResult(0, 0, List.of());
        }

        List<String> errorMessages = new ArrayList<>();
        Set<String> seenInRequest = new HashSet<>();
        List<SingleUserImportDto> validDtos = new ArrayList<>();

        // 1. Kiểm tra trùng lặp ngay trong payload gửi lên
        for (SingleUserImportDto dto : request.users()) {
            if (dto.email() == null || dto.email().isBlank()) {
                errorMessages.add("Email không được để trống.");
                continue;
            }
            String emailLower = dto.email().trim().toLowerCase();
            if (!seenInRequest.add(emailLower)) {
                errorMessages.add("Email " + dto.email() + " bị trùng lặp trong danh sách import.");
                continue;
            }
            validDtos.add(dto);
        }

        if (validDtos.isEmpty()) {
            return new BulkImportResult(0, errorMessages.size(), errorMessages);
        }

        // 2. Query DB đúng 1 câu duy nhất kiểm tra email đã tồn tại (Chống N+1)
        Set<String> requestEmails = validDtos.stream()
                .map(d -> d.email().trim().toLowerCase())
                .collect(Collectors.toSet());
        Set<String> existingEmails = userRepository.findExistingEmailsByEmailIn(requestEmails).stream()
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        List<User> usersToSave = new ArrayList<>();
        List<SingleUserImportDto> dtosToProcess = new ArrayList<>();
        Instant now = Instant.now();

        for (SingleUserImportDto dto : validDtos) {
            String emailLower = dto.email().trim().toLowerCase();
            if (existingEmails.contains(emailLower)) {
                errorMessages.add("Email " + dto.email() + " đã tồn tại trong hệ thống.");
                continue;
            }

            try {
                UserRole ur = UserRole.valueOf(dto.role().name());
                User user = User.builder()
                        .email(dto.email().trim())
                        .passwordHash(passwordEncoder.encode(dto.password()))
                        .role(ur)
                        .verified(true)
                        .active(true)
                        .createdAt(now)
                        .updatedAt(now)
                        .build();
                usersToSave.add(user);
                dtosToProcess.add(dto);
            } catch (Exception e) {
                errorMessages.add("Dữ liệu không hợp lệ cho email " + dto.email() + ": " + e.getMessage());
            }
        }

        if (usersToSave.isEmpty()) {
            return new BulkImportResult(0, errorMessages.size(), errorMessages);
        }

        // 3. Batch Save Users
        List<User> savedUsers = userRepository.saveAll(usersToSave);

        // 4. Batch Create UserProfiles, Gamifications & PushSettings
        List<UserProfile> profilesToSave = new ArrayList<>(savedUsers.size());
        List<UserGamification> gamificationsToSave = new ArrayList<>(savedUsers.size());
        List<UserPushSetting> pushSettingsToSave = new ArrayList<>(savedUsers.size());
        List<Long> savedUserIds = new ArrayList<>(savedUsers.size());

        for (int i = 0; i < savedUsers.size(); i++) {
            User savedUser = savedUsers.get(i);
            SingleUserImportDto dto = dtosToProcess.get(i);
            savedUserIds.add(savedUser.getId());

            String userTz = Time.DEFAULT_TIMEZONE;
            if (dto.timezone() != null && !dto.timezone().isBlank()) {
                try {
                    userTz = ZoneId.of(dto.timezone().trim()).getId();
                } catch (Exception e) {
                    log.warn("Invalid timezone '{}' in bulk import dto, falling back to {}", dto.timezone(), Time.DEFAULT_TIMEZONE);
                }
            }

            UserProfile profile = UserProfile.builder()
                    .user(savedUser)
                    .fullName(dto.fullName())
                    .studentLecturerCode(dto.studentLecturerCode() != null ? dto.studentLecturerCode() : "")
                    .faculty(dto.faculty() != null ? dto.faculty() : "")
                    .major(dto.major() != null ? dto.major() : "")
                    .timezone(userTz)
                    .avatarUrl("")
                    .coverUrl("")
                    .avatarFrameUrl("")
                    .gender(dto.gender())
                    .dateOfBirth(dto.dateOfBirth())
                    .bio("Nơi nào có sự sống, nơi đó có công lý!")
                    .totalProposedQuestion(0)
                    .totalApprovedQuestions(0)
                    .badgesCount(0)
                    .friendsCount(0)
                    .followersCount(0)
                    .followingCount(0)
                    .profileCompleted(true)
                    .build();
            profilesToSave.add(profile);

            UserGamification gamification = UserGamification.builder()
                    .user(savedUser)
                    .publicPoints(0.0)
                    .secretPoints(0.0)
                    .coinBalance(0.0)
                    .currentStreak(0)
                    .build();
            gamificationsToSave.add(gamification);

            UserPushSetting pushSetting = UserPushSetting.builder()
                    .user(savedUser)
                    .preferences(createDefaultPushPreferences())
                    .updatedAt(now)
                    .build();
            pushSettingsToSave.add(pushSetting);
        }

        userProfileRepository.saveAll(profilesToSave);
        userGamificationRepository.saveAll(gamificationsToSave);
        userPushSettingRepository.saveAll(pushSettingsToSave);

        // 5. Bắn 1 sự kiện Bulk duy nhất sang Elasticsearch
        eventPublisher
                .publishEvent(EntitySearchSyncEvent.upsertBatch(EntitySearchSyncEvent.EntityType.USER, savedUserIds));

        return new BulkImportResult(savedUsers.size(), errorMessages.size(), errorMessages);
    }

    private PushPreferences createDefaultPushPreferences() {
        return PushPreferences.createDefault();
    }

    @Override
    @Transactional
    public void logout() {

        JwtPayload payload = JwtPayload.getCurrentUserPayload();
        Long currentUserId = payload.getUserId();
        String email = payload.getUsername();
        String deviceId = payload.getDeviceId();

        // Deactive thiết bị của chính user này trong DB
        userDeviceRepository.findByUserIdAndDeviceId(currentUserId, deviceId)
                .ifPresent(device -> {
                    device.setActive(false);
                    device.setFcmToken(null); // Xóa token push notification
                    userDeviceRepository.save(device);
                });

        // Xóa Refresh Token của thiết bị này trong Redis
        String redisKey = "rt:" + email + ":" + deviceId;
        redisTemplate.delete(redisKey);
    }

    @Override
    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        JwtPayload payload = JwtPayload.getCurrentUserPayload();
        Long userId = payload.getUserId();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ResponseCode.USER_NOT_FOUND));

        if (!passwordEncoder.matches(request.oldPassword(), user.getPasswordHash())) {
            throw new AppException(ResponseCode.INCORRECT_IDENTIFIER);
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setUpdatedAt(Instant.now());

        userRepository.save(user);

    }

    @Override
    @Transactional
    public RefreshTokenResponse refreshToken(RefreshTokenRequest request) {

        // Kiểm tra xem có Refresh Token trong Cookie không (Dành cho WEB)
        String cookieRt = cookieUtils.getCookieValue("refreshToken");
        boolean isWebClient = (cookieRt != null && !cookieRt.isBlank());

        String incomingRt = isWebClient ? cookieRt : (request != null ? request.refreshToken() : null);

        // Decode
        JwtPayload payload = jwtService.decodeToken(incomingRt);
        String email = payload.getUsername();
        String deviceId = payload.getDeviceId();

        // Kiểm tra RT trong Redis
        String redisKey = "rt:" + email + ":" + deviceId;
        String savedRt = redisTemplate.opsForValue().get(redisKey);

        if (savedRt == null || !savedRt.equals(incomingRt)) {
            throw new AppException(ResponseCode.TOKEN_INVALID_OR_EXPIRED);
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ResponseCode.USER_NOT_FOUND));

        if (!user.isActive()) {
            throw new AppException(ResponseCode.ACCOUNT_NOT_ACTIVE);
        }

        // Sinh cặp Token mới (Refresh Token Rotation - RTR)
        Map<String, String> rtAtResult = generateAtAndRt(user.getId(), user.getRole(), email, deviceId);
        String newAccessToken = rtAtResult.get("accessToken");
        String newRefreshToken = rtAtResult.get("refreshToken");

        // Cập nhật RT mới vào Redis
        redisTemplate.opsForValue().set(
                redisKey,
                newRefreshToken,
                Duration.ofMillis(jwtService.getRefreshTokenExpiration()));

        // Xử lý trả về tương ứng cho WEB hoặc MOBILE
        if (isWebClient) {
            // Ghi đè HttpOnly Cookie mới cho WEB
            cookieUtils.addHttpOnlyCookie(
                    "refreshToken",
                    newRefreshToken,
                    Duration.ofMillis(jwtService.getRefreshTokenExpiration()));
            // Trả về Body JSON có refreshToken = null cho WEB
            return RefreshTokenResponse.builder()
                    .accessToken(newAccessToken)
                    .refreshToken(null)
                    .build();
        }

        // Đối với MOBILE: Trả về cả 2 token trong Body JSON
        return RefreshTokenResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .build();

    }

    @Override
    @Transactional
    public void adminResetPassword(AdminResetPasswordRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new AppException(ResponseCode.USER_NOT_FOUND));

        String targetPassword = (request.newPassword() != null && !request.newPassword().isBlank())
                ? request.newPassword()
                : "Hust@123456";

        user.setPasswordHash(passwordEncoder.encode(targetPassword));
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);

        // Đăng xuất khỏi tất cả các thiết bị
        Set<String> keys = redisTemplate.keys("rt:" + request.email() + ":*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

}
