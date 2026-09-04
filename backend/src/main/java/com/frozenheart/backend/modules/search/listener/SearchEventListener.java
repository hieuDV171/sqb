package com.frozenheart.backend.modules.search.listener;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.frozenheart.backend.core.dto.event.EntitySearchSyncEvent;
import com.frozenheart.backend.modules.search.document.PostSearchDoc;
import com.frozenheart.backend.modules.search.document.QuestionSearchDoc;
import com.frozenheart.backend.modules.search.document.SessionSearchDoc;
import com.frozenheart.backend.modules.search.document.SubjectSearchDoc;
import com.frozenheart.backend.modules.search.document.UserSearchDoc;
import com.frozenheart.backend.modules.search.service.SearchSyncService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class SearchEventListener {

    private final SearchSyncService syncService;

    @Async 
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleEntitySearchSyncEvent(EntitySearchSyncEvent event) {
        if (event == null || event.entityType() == null || event.entityIds() == null || event.entityIds().isEmpty()) {
            return;
        }

        log.info("[SearchEventListener] 📩 Nhận event đồng bộ ES: {} Số lượng ID: {} - Hành động: {}",
                event.entityType(), event.entityIds().size(), event.action());

        if (event.action() == EntitySearchSyncEvent.SyncAction.DELETE) {
            String indexName = resolveIndexName(event.entityType());
            if (indexName != null) {
                if (event.entityIds().size() == 1) {
                    syncService.deleteDocument(indexName, event.entityIds().get(0));
                } else {
                    syncService.deleteDocumentsBatch(indexName, event.entityIds());
                }
            }
            return;
        }

        // Xử lý UPSERT: Đơn lẻ vs Hàng loạt
        if (event.entityIds().size() == 1) {
            Long singleId = event.entityIds().get(0);
            switch (event.entityType()) {
                case USER -> syncService.syncUser(singleId);
                case SUBJECT -> syncService.syncSubject(singleId);
                case POST -> syncService.syncPost(singleId);
                case SESSION -> syncService.syncSession(singleId);
                case QUESTION -> syncService.syncQuestion(singleId);
            }
        } else {
            switch (event.entityType()) {
                case POST -> syncService.syncPostsBatch(event.entityIds());
                case QUESTION -> syncService.syncQuestionsBatch(event.entityIds());
                case SESSION -> syncService.syncSessionsBatch(event.entityIds());
                case USER -> syncService.syncUsersBatch(event.entityIds());
                case SUBJECT -> syncService.syncSubjectsBatch(event.entityIds());
            }
        }
    }
    
    private String resolveIndexName(EntitySearchSyncEvent.EntityType type) {
        return switch (type) {
            case USER -> UserSearchDoc.INDEX_NAME;
            case SUBJECT -> SubjectSearchDoc.INDEX_NAME;
            case POST -> PostSearchDoc.INDEX_NAME;
            case SESSION -> SessionSearchDoc.INDEX_NAME;
            case QUESTION -> QuestionSearchDoc.INDEX_NAME;
        };
    }

}
