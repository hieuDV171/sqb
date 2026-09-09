package com.frozenheart.backend.modules.media.job;

import com.frozenheart.backend.core.config.property.MinioProperties;
import com.frozenheart.backend.core.constant.Time;
import com.frozenheart.backend.core.entity.conversation.Message;
import com.frozenheart.backend.core.entity.media.MediaItem;
import com.frozenheart.backend.modules.conversation.repository.MessageRepository;
import com.frozenheart.backend.modules.media.service.MediaService;
import io.minio.ListObjectsArgs;
import io.minio.MinioClient;
import io.minio.Result;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class StorageReconciliationCronJob {

    private final MinioClient minioClient;
    private final MinioProperties minioProperties;
    private final MessageRepository messageRepository;
    private final MediaService mediaService;

    private static final int BATCH_SIZE = 100;

    /**
     * Cronjob chạy lúc 03:00 sáng Chủ nhật hàng tuần theo múi giờ Việt Nam (Asia/Ho_Chi_Minh).
     */
    @Scheduled(cron = "0 0 3 * * SUN", zone = Time.DEFAULT_TIMEZONE)
    public void runWeeklyStorageReconciliation() {
        log.info("[StorageReconciliationCronJob] 🚀 Bắt đầu chu trình quét dọn storage định kỳ hàng tuần...");

        cleanRevokedMessagesMedia();
        cleanStaleExportFiles();

        log.info("[StorageReconciliationCronJob] 🏁 Hoàn tất chu trình quét dọn storage định kỳ hàng tuần.");
    }

    /**
     * Tác vụ 1: Dọn dẹp media của các tin nhắn chat đã bị thu hồi quá 30 ngày.
     */
    public void cleanRevokedMessagesMedia() {
        log.info("[StorageReconciliationCronJob] Đang quét các tin nhắn đã thu hồi quá 30 ngày...");
        Instant thirtyDaysAgo = Instant.now().minus(30, ChronoUnit.DAYS);
        Pageable pageable = PageRequest.of(0, BATCH_SIZE);

        int totalMessagesCleaned = 0;
        int totalFilesCleaned = 0;

        while (true) {
            List<Message> revokedMessages = messageRepository.findRevokedMessagesWithMediaOlderThan(thirtyDaysAgo, pageable);
            if (revokedMessages.isEmpty()) {
                break;
            }

            List<String> urlsToDelete = new ArrayList<>();
            for (Message m : revokedMessages) {
                if (m.getMediaUrls() != null) {
                    for (MediaItem item : m.getMediaUrls()) {
                        String urlOrKey = (item.key() != null && !item.key().isBlank()) ? item.key() : item.url();
                        if (urlOrKey != null && !urlOrKey.isBlank()) {
                            urlsToDelete.add(urlOrKey);
                        }
                    }
                }
                m.setMediaUrls(null);
            }

            messageRepository.saveAll(revokedMessages);

            if (!urlsToDelete.isEmpty()) {
                mediaService.deleteMedia(urlsToDelete);
                totalFilesCleaned += urlsToDelete.size();
            }

            totalMessagesCleaned += revokedMessages.size();

            // Nếu số lượng tin nhắn trong batch nhỏ hơn BATCH_SIZE thì đã duyệt hết
            if (revokedMessages.size() < BATCH_SIZE) {
                break;
            }
        }

        log.info("[StorageReconciliationCronJob] Đã dọn dẹp media của {} tin nhắn thu hồi (>30 ngày), tổng cộng {} file MinIO.",
                totalMessagesCleaned, totalFilesCleaned);
    }

    /**
     * Tác vụ 2: Dọn dẹp các file cache xuất đề thi (exports/) cũ hơn 7 ngày.
     */
    public void cleanStaleExportFiles() {
        log.info("[StorageReconciliationCronJob] Đang quét các file export cache cũ hơn 7 ngày...");
        Instant sevenDaysAgo = Instant.now().minus(7, ChronoUnit.DAYS);
        List<String> exportKeysToDelete = new ArrayList<>();

        try {
            Iterable<Result<Item>> results = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(minioProperties.getBucketName())
                            .prefix("exports/")
                            .recursive(true)
                            .build()
            );

            for (Result<Item> result : results) {
                Item item = result.get();
                ZonedDateTime lastModified = item.lastModified();
                if (lastModified != null && lastModified.toInstant().isBefore(sevenDaysAgo)) {
                    exportKeysToDelete.add(item.objectName());
                }
            }

            if (!exportKeysToDelete.isEmpty()) {
                log.info("[StorageReconciliationCronJob] Tìm thấy {} file export cũ quá 7 ngày, tiến hành xóa...",
                        exportKeysToDelete.size());
                mediaService.deleteMedia(exportKeysToDelete);
            } else {
                log.info("[StorageReconciliationCronJob] Không có file export nào quá hạn 7 ngày.");
            }
        } catch (Exception e) {
            log.error("[StorageReconciliationCronJob] Lỗi khi quét dọn các file exports trên MinIO: ", e);
        }
    }
}
