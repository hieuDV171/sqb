package com.frozenheart.backend.modules.report.service;

import com.frozenheart.backend.modules.post.repository.PostRepository;
import com.frozenheart.backend.modules.report.repository.ModerationActionRepository;
import com.frozenheart.backend.modules.search.service.SearchSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;

@Component
@RequiredArgsConstructor
@Slf4j
public class ModerationPostSync {
    public record Requested(Long actionId) {}

    private final ModerationActionRepository actions;
    private final PostRepository posts;
    private final SearchSyncService search;
    private final TransactionTemplate transactions;

    @Async
    @TransactionalEventListener
    public void afterCommit(Requested request) {
        sync(request.actionId());
    }

    @Scheduled(fixedDelayString = "PT1M")
    public void retry() {
        actions.findPendingPostSync(PageRequest.of(0, 50)).forEach(this::sync);
    }

    private void sync(Long id) {
        try {
            transactions.executeWithoutResult(status -> actions.findById(id).ifPresent(action -> {
                if (action.getSearchSyncedAt() != null) return;
                // Order competing syncs and decisions on the same row. Every retry reads the current state.
                posts.findForModeration(action.getTargetId());
                search.syncModeratedPost(action.getTargetId());
                action.setSearchSyncedAt(Instant.now());
            }));
        } catch (RuntimeException failure) {
            log.warn("Could not sync moderated post for action {}", id, failure);
        }
    }
}
