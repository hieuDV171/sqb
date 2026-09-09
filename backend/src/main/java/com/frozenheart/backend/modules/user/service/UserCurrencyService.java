package com.frozenheart.backend.modules.user.service;

import com.frozenheart.backend.core.entity.user.CoinTransactionTargetType;
import com.frozenheart.backend.core.entity.user.CoinTransactionType;

public interface UserCurrencyService {

    double getBalance(Long userId);

    void deduct(Long userId, double amount, CoinTransactionType type, String description, CoinTransactionTargetType targetType, Long targetId);

    void deduct(Long userId, double amount, String reason);

    void add(Long userId, double amount, CoinTransactionType type, String description, CoinTransactionTargetType targetType, Long targetId);

    void add(Long userId, double amount, String reason);
}
