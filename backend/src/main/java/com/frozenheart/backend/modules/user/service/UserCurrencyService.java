package com.frozenheart.backend.modules.user.service;

public interface UserCurrencyService {

    double getBalance(Long userId);

    void deduct(Long userId, double amount, String reason);

    void add(Long userId, double amount, String reason);
}
