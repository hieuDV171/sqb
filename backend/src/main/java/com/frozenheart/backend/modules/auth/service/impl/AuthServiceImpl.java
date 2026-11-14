package com.frozenheart.backend.modules.auth.service.impl;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
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
import com.frozenheart.backend.modules.auth.dto.user.ConfirmEmailVerificationRequest;
import com.frozenheart.backend.modules.auth.dto.user.LoginRequest;
import com.frozenheart.backend.modules.auth.dto.user.RefreshTokenRequest;
import com.frozenheart.backend.modules.auth.dto.user.RefreshTokenResponse;
import com.frozenheart.backend.modules.auth.service.AuthService;
import com.frozenheart.backend.modules.auth.service.EmailService;
import com.frozenheart.backend.modules.device.repository.UserDeviceRepository;
import com.frozenheart.backend.modules.user.repository.UserProfileRepository;
import com.frozenheart.backend.modules.user.repository.UserPushSettingRepository;
import com.frozenheart.backend.modules.user.repository.UserRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
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
import com.frozenheart.backend.core.security.AccountState;
import com.frozenheart.backend.core.security.TokenType;
import com.frozenheart.backend.core.util.CookieUtils;
import com.frozenheart.backend.core.util.MetricService;

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
    private final MetricService metricService;

    private final ApplicationEventPublisher eventPublisher;

    @Value("${app.security.default-user-password}")
    private String defaultUserPassword;

    private static final String OTP_PREFIX = "otp:";
    private static final String RESEND_LOCK_PREFIX = "resend_lock:";
    private static final String OTP_ATTEMPT_PREFIX = "otp_attempt:";
    private static final Duration OTP_TTL = Duration.ofMinutes(2);
    private static final int OTP_MAX_ATTEMPTS = 5;

    @Transactional
    public AuthResponse login(LoginRequest request) {
        String email = request.email();

        User user = userRepository.findByEmailForLogin(email)
                .orElseThrow(() -> {
                    metricService.incrementCounter("sqb.auth.login", "status", "failed", "reason", "user_not_found");
                    return new AppException(ResponseCode.INCORRECT_IDENTIFIER, "Tài khoản hoặc mật khẩu không chính xác");
                });

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            log.warn("[AuthServiceImpl]: {}", ResponseCode.INCORRECT_IDENTIFIER);
            metricService.incrementCounter("sqb.auth.login", "status", "failed", "reason", "bad_credentials");
            throw new AppException(ResponseCode.INCORRECT_IDENTIFIER, "Tài khoản hoặc mật khẩu không chính xác");
        }

        Long userId = user.getId();

        UserProfile userProfile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new AppException(ResponseCode.USER_NOT_FOUND));

        if (!user.isActive()) {
            metricService.incrementCounter("sqb.auth.login", "status", "failed", "reason", "not_active");
            throw new AppException(ResponseCode.ACCOUNT_NOT_ACTIVE, "Tài khoản đã bị khóa");
        }
        if (user.isSuspendedAt(Instant.now())) {
            var action = user.getActiveModerationAction();
            String reason = action == null ? "" : " Lý do: " + action.getReason();
            throw new AppException(ResponseCode.ACCOUNT_SUSPENDED, "Tài khoản bị đình chỉ đến "
                    + DateTimeFormatter.ofPattern("HH:mm dd/MM/uuuu")
                            .withZone(ZoneId.of(Time.DEFAULT_TIMEZONE)).format(user.getSuspendedUntil()) + "." + reason);
        }

        metricService.incrementCounter("sqb.auth.login", "status", "success", "reason", "none");

        saveOrUpdateUserDevice(user, request,
                AccountState.from(user.isVerified(), userProfile.isProfileCompleted()));

        AuthResponse authResponse = buildAuthResponse(request.deviceId(), user, userProfile);
        if (authResponse.tokenType() == TokenType.ONBOARDING) {
            log.info("[AuthService] Issued restricted onboarding session for userId={} state={}",
                    userId, authResponse.accountState());
        }

        if (request.platform() == DevicePlatform.WEB && authResponse.tokenType() == TokenType.ACCESS) {
            cookieUtils.addHttpOnlyCookie(
                    "refreshToken",
                    authResponse.refreshToken(),
                    Duration.ofMillis(jwtService.getRefreshTokenExpiration()));

            return AuthResponse.builder()
                    .userId(authResponse.userId())
                    .username(authResponse.username())
                    .role(authResponse.role())
                    .accessToken(authResponse.accessToken())
                    .refreshToken(null) // WEB không nhận refreshToken trong JSON Body
                    .avatarUrl(authResponse.avatarUrl())
                    .coverUrl(authResponse.coverUrl())
                    .frameUrl(authResponse.frameUrl())
                    .verified(authResponse.verified())
                    .profileCompleted(authResponse.profileCompleted())
                    .tokenType(authResponse.tokenType())
                    .accountState(authResponse.accountState())
                    .build();
        }

        if (request.platform() == DevicePlatform.WEB) {
            cookieUtils.clearCookie("refreshToken");
        }

        return authResponse;

    }

    private void saveOrUpdateUserDevice(User user, LoginRequest request, AccountState accountState) {
        String fid = accountState == AccountState.ACTIVE ? request.fid() : null;
        if (fid != null && !fid.isBlank()) {
            userDeviceRepository.lockFidForTransaction(fid);
            if (userDeviceRepository.isFidBoundToAnotherDevice(fid, user.getId(), request.deviceId())) {
                fid = null;
            }
        }

        // Client cung cấp deviceId, nên chỉ cập nhật thiết bị thuộc tài khoản đăng nhập.
        UserDevice device = userDeviceRepository.findByUserIdAndDeviceId(user.getId(), request.deviceId())
                .orElseGet(() -> UserDevice.builder()
                        .user(user)
                        .deviceId(request.deviceId())
                        .createdAt(Instant.now())
                        .build());

        device.setFid(fid == null || fid.isBlank() ? null : fid);
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
                .schoolFaculty("")
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
        redisTemplate.opsForValue().set(OTP_PREFIX + email, otpCode, OTP_TTL);
        redisTemplate.delete(OTP_ATTEMPT_PREFIX + email);

        emailService.sendOtpEmail(email, otpCode);
    }

    @Override
    @Transactional
    public AuthResponse verify(VerifyOtpRequest request) {
        String email = request.email();
        String savedOtp = redisTemplate.opsForValue().get(OTP_PREFIX + email);

        // Kiểm tra mã OTP
        if (savedOtp == null || !savedOtp.equals(request.verifyCode())) {
            metricService.incrementCounter("sqb.auth.otp", "action", "verify", "status", "failed");
            recordInvalidOtpAttempt(email);
        }

        // Cập nhật thông tin User thành verified
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ResponseCode.USER_NOT_FOUND));

        user.setVerified(true);
        userRepository.save(user);

        metricService.incrementCounter("sqb.auth.otp", "action", "verify", "status", "success");

        // Xóa OTP trong Redis sau khi đã verify thành công
        redisTemplate.delete(OTP_PREFIX + email);
        redisTemplate.delete(OTP_ATTEMPT_PREFIX + email);

        String deviceId = request.deviceId();

        Long userId = user.getId();
        UserProfile userProfile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new AppException(ResponseCode.USER_NOT_FOUND));
        eventPublisher.publishEvent(EntitySearchSyncEvent.upsert(EntitySearchSyncEvent.EntityType.USER, user.getId()));

        return buildAuthResponse(deviceId, user, userProfile);

    }

    private Map<String, String> generateAtAndRt(
            Long userId,
            UserRole role,
            String email,
            String deviceId,
            AccountState accountState,
            String sessionId,
            Long sessionEpoch
    ) {

        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("userId", userId);
        extraClaims.put("deviceId", deviceId);
        if (sessionEpoch != null) extraClaims.put("sessionEpoch", sessionEpoch);

        String accessToken;
        String refreshToken = null;
        if (accountState == AccountState.ACTIVE) {
            extraClaims.put("sessionId", sessionId);
            extraClaims.put("role", role.name());
            extraClaims.put("tokenType", TokenType.ACCESS.name());
            extraClaims.put("accountState", AccountState.ACTIVE.name());
            accessToken = jwtService.generateAccessToken(extraClaims, email);
            refreshToken = sessionEpoch == null ? jwtService.generateRefreshToken(email, userId, deviceId, sessionId)
                    : jwtService.generateRefreshToken(email, userId, deviceId, sessionId, sessionEpoch);
        } else {
            extraClaims.put("tokenType", TokenType.ONBOARDING.name());
            extraClaims.put("accountState", accountState.name());
            accessToken = jwtService.generateOnboardingToken(extraClaims, email);
        }

        Map<String, String> result = new HashMap<>();
        result.put("accessToken", accessToken);
        result.put("refreshToken", refreshToken);

        return result;
    }

    private AuthResponse buildAuthResponse(String deviceId, User user, UserProfile userProfile) {

        Long userId = user.getId();
        UserRole role = user.getRole();
        String email = user.getEmail();

        AccountState accountState = AccountState.from(user.isVerified(), userProfile.isProfileCompleted());
        Map<String, String> rtAtResult = generateAtAndRt(userId, role, email, deviceId, accountState,
                UUID.randomUUID().toString(), user.getSessionEpoch());

        String accessToken = rtAtResult.get("accessToken");
        String refreshToken = rtAtResult.get("refreshToken");

        if (refreshToken != null) {
            String redisKey = "rt:" + email + ":" + deviceId;
            redisTemplate.opsForValue().set(
                    redisKey,
                    refreshToken,
                    Duration.ofMillis(jwtService.getRefreshTokenExpiration()));
        }

        return AuthResponse.builder()
                .userId(userId)
                .username(user.getEmail())
                .role(role)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .avatarUrl(userProfile.getAvatarUrl())
                .coverUrl(userProfile.getCoverUrl())
                .frameUrl(userProfile.getAvatarFrameUrl())
                .verified(user.isVerified())
                .profileCompleted(userProfile.isProfileCompleted())
                .tokenType(accountState == AccountState.ACTIVE ? TokenType.ACCESS : TokenType.ONBOARDING)
                .accountState(accountState)
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
        redisTemplate.opsForValue().set(OTP_PREFIX + email, newOtp, OTP_TTL);
        redisTemplate.delete(OTP_ATTEMPT_PREFIX + email);
        redisTemplate.opsForValue().set(lockKey, "1", Duration.ofSeconds(60));

        emailService.sendOtpEmail(email, newOtp);
        metricService.incrementCounter("sqb.auth.otp", "action", "resend", "status", "success");
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
        Set<String> seenEmailsInRequest = new HashSet<>();
        Set<String> seenCodesInRequest = new HashSet<>();
        List<SingleUserImportDto> validDtos = new ArrayList<>();

        // 1. Kiểm tra trùng lặp ngay trong payload gửi lên
        for (SingleUserImportDto dto : request.users()) {
            if (dto.email() == null || dto.email().isBlank()) {
                errorMessages.add("Email không được để trống.");
                continue;
            }
            String emailLower = dto.email().trim().toLowerCase();
            if (!seenEmailsInRequest.add(emailLower)) {
                errorMessages.add("Email " + dto.email() + " bị trùng lặp trong danh sách import.");
                continue;
            }
            if (dto.studentLecturerCode() != null && !dto.studentLecturerCode().isBlank()) {
                String codeLower = dto.studentLecturerCode().trim().toLowerCase();
                if (!seenCodesInRequest.add(codeLower)) {
                    errorMessages.add("Mã cán bộ/SV " + dto.studentLecturerCode() + " bị trùng lặp trong danh sách import.");
                    continue;
                }
            }
            validDtos.add(dto);
        }

        if (validDtos.isEmpty()) {
            return new BulkImportResult(0, errorMessages.size(), errorMessages);
        }

        // 2. Query DB đúng 1 câu duy nhất kiểm tra email và mã cán bộ đã tồn tại (Chống N+1)
        Set<String> requestEmails = validDtos.stream()
                .map(d -> d.email().trim().toLowerCase())
                .collect(Collectors.toSet());
        Set<String> existingEmails = userRepository.findExistingEmailsByEmailIn(requestEmails).stream()
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        Set<String> requestCodes = validDtos.stream()
                .map(SingleUserImportDto::studentLecturerCode)
                .filter(c -> c != null && !c.isBlank())
                .map(String::trim)
                .collect(Collectors.toSet());
        Set<String> existingCodes = requestCodes.isEmpty() ? Collections.emptySet()
                : userProfileRepository.findAllByStudentLecturerCodeIn(requestCodes).stream()
                        .map(UserProfile::getStudentLecturerCode)
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

            String code = dto.studentLecturerCode() != null ? dto.studentLecturerCode().trim() : "";
            if (!code.isBlank() && existingCodes.contains(code.toLowerCase())) {
                errorMessages.add("Mã cán bộ/SV " + code + " đã tồn tại trong hệ thống.");
                continue;
            }

            try {
                // Role: Nếu không chỉ định -> Mặc định là LECTURER (tối ưu cho import Giảng viên)
                UserRole ur = (dto.role() != null) ? UserRole.valueOf(dto.role().name()) : UserRole.LECTURER;

                // Password: Nếu không chỉ định -> Tự sinh thông minh: Ngày sinh ddMMyyyy -> Mã CB -> mật khẩu mặc định
                String rawPassword;
                if (dto.password() != null && !dto.password().isBlank()) {
                    rawPassword = dto.password().trim();
                } else if (dto.dateOfBirth() != null) {
                    rawPassword = dto.dateOfBirth().format(DateTimeFormatter.ofPattern("ddMMyyyy"));
                } else if (!code.isBlank()) {
                    rawPassword = code;
                } else {
                    rawPassword = defaultUserPassword;
                }

                User user = User.builder()
                        .email(dto.email().trim())
                        .passwordHash(passwordEncoder.encode(rawPassword))
                        .role(ur)
                        .verified(false)
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

            String faculty = dto.schoolFaculty() != null ? dto.schoolFaculty().trim() : "";
            String bio;
            if (savedUser.getRole() == UserRole.LECTURER) {
                bio = !faculty.isBlank() ? "Giảng viên tại " + faculty : "Giảng viên";
            } else if (savedUser.getRole() == UserRole.ADMIN) {
                bio = "Quản trị viên hệ thống";
            } else {
                bio = "Nơi nào có sự sống, nơi đó có công lý!";
            }

            UserProfile profile = UserProfile.builder()
                    .user(savedUser)
                    .fullName(dto.fullName().trim())
                    .studentLecturerCode(dto.studentLecturerCode() != null ? dto.studentLecturerCode().trim() : "")
                    .schoolFaculty(faculty)
                    .major(dto.major() != null ? dto.major().trim() : "")
                    .className(dto.className() != null ? dto.className().trim() : "")
                    .timezone(userTz)
                    .avatarUrl("")
                    .coverUrl("")
                    .avatarFrameUrl("")
                    .gender(dto.gender())
                    .dateOfBirth(dto.dateOfBirth())
                    .bio(bio)
                    .totalProposedQuestion(0)
                    .totalApprovedQuestions(0)
                    .badgesCount(0)
                    .friendsCount(0)
                    .followersCount(0)
                    .followingCount(0)
                    .profileCompleted(false)
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
                    device.setFid(null); // Xóa FID push notification
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

        UserProfile userProfile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new AppException(ResponseCode.USER_NOT_FOUND));

        if (!userProfile.isProfileCompleted() && !user.isVerified()) {
            throw new AppException(
                    ResponseCode.ACCOUNT_ONBOARDING_REQUIRED,
                    "Vui lòng xác thực email trước khi đổi mật khẩu"
            );
        }

        if (!passwordEncoder.matches(request.oldPassword(), user.getPasswordHash())) {
            throw new AppException(ResponseCode.INCORRECT_IDENTIFIER);
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setUpdatedAt(Instant.now());

        userRepository.save(user);

        userProfile.setProfileCompleted(true);
        userProfileRepository.save(userProfile);
        if (user.isVerified()) {
            log.info("[AuthService] Account setup completed for userId={}; re-login required", userId);
        }

    }

    @Override
    public void requestEmailVerification() {
        Long userId = JwtPayload.getCurrentUserPayload().getUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ResponseCode.USER_NOT_FOUND));

        if (user.isVerified()) {
            throw new AppException(ResponseCode.ACTION_ALREADY_PERFORMED, "Email đã được xác thực");
        }

        sendVerificationOtp(user.getEmail());
    }

    @Override
    @Transactional
    public void confirmEmailVerification(ConfirmEmailVerificationRequest request) {
        Long userId = JwtPayload.getCurrentUserPayload().getUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ResponseCode.USER_NOT_FOUND));

        if (user.isVerified()) {
            throw new AppException(ResponseCode.ACTION_ALREADY_PERFORMED, "Email đã được xác thực");
        }

        String email = user.getEmail();
        String savedOtp = redisTemplate.opsForValue().get(OTP_PREFIX + email);
        if (savedOtp == null || !savedOtp.equals(request.verifyCode())) {
            metricService.incrementCounter("sqb.auth.otp", "action", "self_verify", "status", "failed");
            recordInvalidOtpAttempt(email);
        }

        user.setVerified(true);
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);
        redisTemplate.delete(OTP_PREFIX + email);
        redisTemplate.delete(RESEND_LOCK_PREFIX + email);
        redisTemplate.delete(OTP_ATTEMPT_PREFIX + email);
        metricService.incrementCounter("sqb.auth.otp", "action", "self_verify", "status", "success");
        eventPublisher.publishEvent(EntitySearchSyncEvent.upsert(EntitySearchSyncEvent.EntityType.USER, userId));
        log.info("[AuthService] Email ownership verified for userId={}", userId);
    }

    private void sendVerificationOtp(String email) {
        String lockKey = RESEND_LOCK_PREFIX + email;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(lockKey))) {
            throw new AppException(ResponseCode.ACTION_ALREADY_PERFORMED);
        }

        String newOtp = generateOtpCode();
        redisTemplate.opsForValue().set(OTP_PREFIX + email, newOtp, OTP_TTL);
        redisTemplate.delete(OTP_ATTEMPT_PREFIX + email);
        redisTemplate.opsForValue().set(lockKey, "1", Duration.ofSeconds(60));
        emailService.sendOtpEmail(email, newOtp);
        metricService.incrementCounter("sqb.auth.otp", "action", "self_request", "status", "success");
    }

    private void recordInvalidOtpAttempt(String email) {
        String attemptKey = OTP_ATTEMPT_PREFIX + email;
        Long attempts = redisTemplate.opsForValue().increment(attemptKey);
        if (attempts != null && attempts == 1L) {
            redisTemplate.expire(attemptKey, OTP_TTL);
        }
        if (attempts != null && attempts >= OTP_MAX_ATTEMPTS) {
            redisTemplate.delete(OTP_PREFIX + email);
            throw new AppException(
                    ResponseCode.OTP_ATTEMPT_LIMIT_EXCEEDED,
                    "Bạn đã nhập sai OTP quá 5 lần. Vui lòng yêu cầu mã mới"
            );
        }
        throw new AppException(ResponseCode.VERIFICATION_CODE_INVALID);
    }

    @Override
    @Transactional
    public RefreshTokenResponse refreshToken(RefreshTokenRequest request) {

        // Kiểm tra xem có Refresh Token trong Cookie không (Dành cho WEB)
        String cookieRt = cookieUtils.getCookieValue("refreshToken");
        boolean isWebClient = (cookieRt != null && !cookieRt.isBlank());

        String incomingRt = isWebClient ? cookieRt : (request != null ? request.refreshToken() : null);

        if (incomingRt == null || incomingRt.isBlank()) {
            throw new AppException(ResponseCode.TOKEN_INVALID_OR_EXPIRED);
        }

        // Decode
        JwtPayload payload;
        try {
            payload = jwtService.decodeToken(incomingRt);
        } catch (io.jsonwebtoken.JwtException | IllegalArgumentException invalidToken) {
            throw new AppException(ResponseCode.TOKEN_INVALID_OR_EXPIRED);
        }
        String email = payload.getUsername();
        String deviceId = payload.getDeviceId();

        if (payload.getTokenType() != TokenType.REFRESH) {
            throw new AppException(ResponseCode.TOKEN_INVALID_OR_EXPIRED);
        }

        // Kiểm tra RT trong Redis
        String redisKey = "rt:" + email + ":" + deviceId;
        String savedRt = redisTemplate.opsForValue().get(redisKey);

        if (savedRt == null || !savedRt.equals(incomingRt)) {
            throw new AppException(ResponseCode.TOKEN_INVALID_OR_EXPIRED);
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ResponseCode.USER_NOT_FOUND));

        // Legacy JWTs lack userId. Compare at the precision of their issued-at claim.
        boolean tokenBelongsToAccount = payload.getUserId() != null
                ? payload.getUserId().equals(user.getId())
                : payload.getIssuedAt() != null && (user.getCreatedAt() == null
                        || !payload.getIssuedAt().isBefore(user.getCreatedAt().truncatedTo(ChronoUnit.SECONDS)));
        if (!tokenBelongsToAccount) {
            throw new AppException(ResponseCode.TOKEN_INVALID_OR_EXPIRED);
        }
        if (!user.isActive()) {
            throw new AppException(ResponseCode.ACCOUNT_NOT_ACTIVE);
        }
        if (user.isSuspendedAt(Instant.now()) || !Objects.equals(payload.getSessionEpoch(), user.getSessionEpoch())) {
            throw new AppException(ResponseCode.TOKEN_INVALID_OR_EXPIRED);
        }

        UserProfile userProfile = userProfileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new AppException(ResponseCode.USER_NOT_FOUND));
        if (AccountState.from(user.isVerified(), userProfile.isProfileCompleted()) != AccountState.ACTIVE) {
            redisTemplate.delete(redisKey);
            if (isWebClient) {
                cookieUtils.clearCookie("refreshToken");
            }
            throw new AppException(ResponseCode.ACCOUNT_ONBOARDING_REQUIRED);
        }

        // Sinh cặp Token mới (Refresh Token Rotation - RTR)
        String sessionId = payload.getSessionId() != null ? payload.getSessionId() : UUID.randomUUID().toString();
        Map<String, String> rtAtResult = generateAtAndRt(
                user.getId(), user.getRole(), email, deviceId, AccountState.ACTIVE, sessionId, user.getSessionEpoch());
        String newAccessToken = rtAtResult.get("accessToken");
        String newRefreshToken = rtAtResult.get("refreshToken");

        // Rotate only while the presented token is still current.
        RedisScript<Long> rotation = RedisScript.of("""
                if redis.call('GET', KEYS[1]) ~= ARGV[1] then return 0 end
                redis.call('PSETEX', KEYS[1], %d, ARGV[2])
                return 1
                """.formatted(jwtService.getRefreshTokenExpiration()), Long.class);
        if (!Long.valueOf(1L).equals(redisTemplate.execute(rotation, List.of(redisKey), incomingRt, newRefreshToken))) {
            throw new AppException(ResponseCode.TOKEN_INVALID_OR_EXPIRED);
        }

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
                : defaultUserPassword;

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
