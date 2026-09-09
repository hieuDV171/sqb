package com.frozenheart.backend.modules.media.listener;

import com.frozenheart.backend.core.dto.event.MediaCleanupEvent;
import com.frozenheart.backend.modules.media.service.MediaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class MediaCleanupEventListener {

    private final MediaService mediaService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleMediaCleanupEvent(MediaCleanupEvent event) {
        if (event == null || event.urlsOrKeys() == null || event.urlsOrKeys().isEmpty()) {
            return;
        }

        log.info("[MediaCleanupEventListener] 🗑️ Nhận event dọn dẹp media sau commit, số lượng file: {}",
                event.urlsOrKeys().size());

        try {
            mediaService.deleteMedia(event.urlsOrKeys());
        } catch (Exception e) {
            log.error("[MediaCleanupEventListener] Lỗi khi thực hiện dọn dẹp media: ", e);
        }
    }
}
