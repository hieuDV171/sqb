package com.frozenheart.backend.modules.user.scheduler;

import com.frozenheart.backend.modules.user.service.AccountDeletionService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class AccountDeletionScheduler {

    private final AccountDeletionService accountDeletionService;

    @Scheduled(fixedDelay = 60_000)
    public void processDueAccounts() {
        accountDeletionService.processDueAccounts(Instant.now());
    }
}
