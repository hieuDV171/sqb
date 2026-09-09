package com.frozenheart.backend.modules.user.service.impl;

import com.frozenheart.backend.core.constant.ResponseCode;
import com.frozenheart.backend.core.entity.user.*;
import com.frozenheart.backend.core.exception.AppException;
import com.frozenheart.backend.modules.gamification.repository.CoinTransactionRepository;
import com.frozenheart.backend.modules.gamification.repository.UserGamificationRepository;
import com.frozenheart.backend.modules.user.repository.UserRepository;
import com.frozenheart.backend.modules.user.service.UserCurrencyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserCurrencyServiceImpl implements UserCurrencyService {

    private final UserGamificationRepository userGamificationRepository;
    private final CoinTransactionRepository coinTransactionRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public double getBalance(Long userId) {
        if (userId == null) {
            return 0.0;
        }

        return userGamificationRepository.findById(userId)
                .map(UserGamification::getCoinBalance)
                .orElse(0.0);
    }

    @Override
    @Transactional
    public void deduct(Long userId, double amount, CoinTransactionType type, String description,
                       CoinTransactionTargetType targetType, Long targetId) {
        if (userId == null || amount <= 0) {
            return;
        }

        // Thực hiện trừ tiền bằng câu lệnh Atomic SQL cấp Database (Row-level exclusive lock)
        int updated = userGamificationRepository.deductCoinBalance(userId, amount);
        if (updated == 0) {
            double currentBalance = getBalance(userId);
            if (currentBalance < amount) {
                throw new AppException(ResponseCode.ACTION_NOT_ALLOWED, "Số dư của bạn không đủ để thực hiện giao dịch này");
            }
            throw new AppException(ResponseCode.RESOURCE_NOT_FOUND, "Không tìm thấy thông tin ví của người dùng");
        }

        double balanceAfter = getBalance(userId);
        User userRef = userRepository.getReferenceById(userId);

        // Ghi sổ cái bất biến (CoinTransaction - Ledger)
        CoinTransaction tx = CoinTransaction.builder()
                .user(userRef)
                .amount(-amount)
                .balanceAfter(balanceAfter)
                .type(type != null ? type : CoinTransactionType.BUY_COSMETIC)
                .description(description)
                .targetType(targetType)
                .targetId(targetId)
                .createdAt(Instant.now())
                .build();
        coinTransactionRepository.save(tx);

        log.info("[UserCurrencyServiceImpl] Deducted {} from user {} (Balance after: {}, Reason: {})",
                amount, userId, balanceAfter, description);
    }

    @Override
    @Transactional
    public void deduct(Long userId, double amount, String reason) {
        deduct(userId, amount, CoinTransactionType.BUY_COSMETIC, reason, null, null);
    }

    @Override
    @Transactional
    public void add(Long userId, double amount, CoinTransactionType type, String description,
                    CoinTransactionTargetType targetType, Long targetId) {
        if (userId == null || amount <= 0) {
            return;
        }

        int updated = userGamificationRepository.addCoinBalance(userId, amount);
        if (updated == 0) {
            // Nếu tài khoản cũ chưa có bản ghi gamification, khởi tạo mới
            User userRef = userRepository.getReferenceById(userId);
            UserGamification newGamification = UserGamification.builder()
                    .user(userRef)
                    .coinBalance(amount)
                    .build();
            userGamificationRepository.save(newGamification);
        }

        double balanceAfter = getBalance(userId);
        User userRef = userRepository.getReferenceById(userId);

        // Ghi sổ cái bất biến (CoinTransaction - Ledger)
        CoinTransaction tx = CoinTransaction.builder()
                .user(userRef)
                .amount(amount)
                .balanceAfter(balanceAfter)
                .type(type != null ? type : CoinTransactionType.GAME_REWARD)
                .description(description)
                .targetType(targetType)
                .targetId(targetId)
                .createdAt(Instant.now())
                .build();
        coinTransactionRepository.save(tx);

        log.info("[UserCurrencyService] Added {} to user {} (Balance after: {}, Reason: {})",
                amount, userId, balanceAfter, description);
    }

    @Override
    @Transactional
    public void add(Long userId, double amount, String reason) {
        add(userId, amount, CoinTransactionType.GAME_REWARD, reason, null, null);
    }
}
