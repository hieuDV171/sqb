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

        UserGamification wallet = userGamificationRepository.findWalletForUpdate(userId).orElse(null);
        if (wallet == null || wallet.getCoinBalance() < amount) {
            throw new AppException(ResponseCode.ACTION_NOT_ALLOWED, "Số dư của bạn không đủ để thực hiện giao dịch này");
        }

        wallet.setCoinBalance(wallet.getCoinBalance() - amount);
        recordTransaction(wallet, -amount, type != null ? type : CoinTransactionType.BUY_COSMETIC,
                description, targetType, targetId);
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

        UserGamification wallet = userGamificationRepository.findWalletForUpdate(userId)
                .orElseGet(() -> createWalletIfAbsent(userId));
        wallet.setCoinBalance(wallet.getCoinBalance() + amount);
        recordTransaction(wallet, amount, type != null ? type : CoinTransactionType.GAME_REWARD,
                description, targetType, targetId);
    }

    private UserGamification createWalletIfAbsent(Long userId) {
        // Serialize initialization for older accounts without a wallet row to lock.
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new AppException(ResponseCode.RESOURCE_NOT_FOUND, "Không tìm thấy tài khoản."));
        return userGamificationRepository.findWalletForUpdate(userId)
                .orElseGet(() -> userGamificationRepository.save(UserGamification.builder().user(user).build()));
    }

    private void recordTransaction(UserGamification wallet, double amount, CoinTransactionType type, String description,
                                   CoinTransactionTargetType targetType, Long targetId) {
        CoinTransaction tx = CoinTransaction.builder()
                .user(wallet.getUser())
                .amount(amount)
                .balanceAfter(wallet.getCoinBalance())
                .type(type)
                .description(description)
                .targetType(targetType)
                .targetId(targetId)
                .createdAt(Instant.now())
                .build();
        coinTransactionRepository.save(tx);

        log.info("[UserCurrencyService] Changed balance by {} for user {} (Balance after: {}, Reason: {})",
                amount, wallet.getUserId(), wallet.getCoinBalance(), description);
    }

    @Override
    @Transactional
    public void add(Long userId, double amount, String reason) {
        add(userId, amount, CoinTransactionType.GAME_REWARD, reason, null, null);
    }
}
