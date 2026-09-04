package com.frozenheart.backend.modules.user.service.impl;

import com.frozenheart.backend.core.constant.ResponseCode;
import com.frozenheart.backend.core.entity.user.GamificationPointsJson;
import com.frozenheart.backend.core.entity.user.UserProfile;
import com.frozenheart.backend.core.exception.AppException;
import com.frozenheart.backend.modules.user.repository.UserProfileRepository;
import com.frozenheart.backend.modules.user.service.UserCurrencyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserCurrencyServiceImpl implements UserCurrencyService {

    private final UserProfileRepository userProfileRepository;

    @Override
    @Transactional(readOnly = true)
    public double getBalance(Long userId) {
        if (userId == null) {
            return 0.0;
        }

        UserProfile profile = userProfileRepository.findByUserId(userId).orElse(null);
        if (profile == null || profile.getGamificationPoints() == null) {
            return 0.0;
        }

        return profile.getGamificationPoints().getCoinBalance();
    }

    @Override
    @Transactional
    public void deduct(Long userId, double amount, String reason) {
        if (userId == null || amount <= 0) {
            return;
        }

        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new AppException(ResponseCode.RESOURCE_NOT_FOUND, "Hồ sơ người dùng không tồn tại"));

        GamificationPointsJson points = profile.getGamificationPoints();
        if (points == null) {
            points = new GamificationPointsJson(0.0, 0.0);
            profile.setGamificationPoints(points);
        }

        double currentBalance = points.getCoinBalance();
        if (currentBalance < amount) {
            throw new AppException(ResponseCode.ACTION_NOT_ALLOWED, "Số dư của bạn không đủ để thực hiện giao dịch này");
        }

        // Chỉ trừ số dư ví coinBalance, bảo toàn 100% publicPoints xếp hạng
        points.setCoinBalance(currentBalance - amount);
        userProfileRepository.save(profile);

        log.info("[UserCurrencyServiceImpl] Deducted {} from user {} (Reason: {}) - Remaining coinBalance: {}",
                amount, userId, reason, points.getCoinBalance());
    }

    @Override
    @Transactional
    public void add(Long userId, double amount, String reason) {
        if (userId == null || amount <= 0) {
            return;
        }

        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new AppException(ResponseCode.RESOURCE_NOT_FOUND, "Hồ sơ người dùng không tồn tại"));

        GamificationPointsJson points = profile.getGamificationPoints();
        if (points == null) {
            points = new GamificationPointsJson(0.0, 0.0);
            profile.setGamificationPoints(points);
        }

        points.setCoinBalance(points.getCoinBalance() + amount);
        userProfileRepository.save(profile);

        log.info("[UserCurrencyService] Added {} to user {} (Reason: {}) - New coinBalance: {}",
                amount, userId, reason, points.getCoinBalance());
    }
}
