package com.frozenheart.backend.modules.media.job;

import com.frozenheart.backend.core.config.property.R2Properties;
import com.frozenheart.backend.core.constant.Time;
import com.frozenheart.backend.core.entity.conversation.Message;
import com.frozenheart.backend.core.entity.media.MediaItem;
import com.frozenheart.backend.modules.conversation.repository.MessageRepository;
import com.frozenheart.backend.modules.media.service.MediaService;
import com.frozenheart.backend.modules.media.service.impl.MediaServiceImpl;
import io.minio.ListObjectsArgs;
import io.minio.MinioClient;
import io.minio.Result;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class StorageReconciliationCronJob {

    private final MinioClient minioClient;
    private final R2Properties r2Properties;
    private final MessageRepository messageRepository;
    private final MediaService mediaService;
    private final RedisTemplate<String, String> redisTemplate;

    private static final int BATCH_SIZE = 100;

    /**
     * Cronjob chạy lúc 03:00 sáng Chủ nhật hàng tuần theo múi giờ Việt Nam (Asia/Ho_Chi_Minh).
     */
    @Scheduled(cron = "0 0 3 * * SUN", zone = Time.DEFAULT_TIMEZONE)
    public void runWeeklyStorageReconciliation() {
        log.info("[StorageReconciliationCronJob] 🚀 Bắt đầu chu trình quét dọn storage định kỳ hàng tuần...");

        cleanRevokedMessagesMedia();
        cleanStaleExportFiles();
        cleanStalePendingMedia();

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

        log.info("[StorageReconciliationCronJob] Đã dọn dẹp media của {} tin nhắn thu hồi (>30 ngày), tổng cộng {} file R2.",
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
                            .bucket(r2Properties.getBucketName())
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
            log.error("[StorageReconciliationCronJob] Lỗi khi quét dọn các file exports trên R2: ", e);
        }
    }

    /**
     * Tác vụ 3: Dọn dẹp các tệp media tải lên tạm thời (Pending) bị mồ côi quá 24h.
     * Chạy định kỳ lúc 03:30 sáng hàng ngày theo múi giờ Việt Nam.
     */
    @Scheduled(cron = "0 30 3 * * *", zone = Time.DEFAULT_TIMEZONE)
    public void cleanStalePendingMedia() {
        log.info("[StorageReconciliationCronJob] 🧹 Đang quét các tệp tải lên mồ côi (pending quá 24h)...");
        long cutoff = Instant.now().minus(24, ChronoUnit.HOURS).toEpochMilli();

        int totalCleaned = 0;
        while (true) {
            // Lấy theo từng batch nhỏ (BATCH_SIZE = 100) để không nghẽn mạng hay nghẽn Redis
            Set<String> expiredKeys = redisTemplate.opsForZSet().rangeByScore(
                    MediaServiceImpl.REDIS_PENDING_MEDIA_KEY, 0, cutoff, 0, BATCH_SIZE);
            if (expiredKeys == null || expiredKeys.isEmpty()) {
                break;
            }

            log.info("[StorageReconciliationCronJob] Đang dọn dẹp batch {} tệp pending quá hạn 24h...", expiredKeys.size());
            mediaService.deleteMedia(new ArrayList<>(expiredKeys));

            // Chỉ xóa đúng các key trong batch vừa được xóa thành công trên R2
            redisTemplate.opsForZSet().remove(
                    MediaServiceImpl.REDIS_PENDING_MEDIA_KEY, expiredKeys.toArray());
            totalCleaned += expiredKeys.size();

            // Nếu số lượng ít hơn BATCH_SIZE tức là đã vét sạch toàn bộ hàng đợi
            if (expiredKeys.size() < BATCH_SIZE) {
                break;
            }
        }

        if (totalCleaned > 0) {
            log.info("[StorageReconciliationCronJob] 🏁 Đã dọn dẹp sạch tổng cộng {} tệp pending mồ côi khỏi Cloudflare R2 và Redis queue.", totalCleaned);
        } else {
            log.info("[StorageReconciliationCronJob] Không có tệp pending nào quá hạn 24h.");
        }
    }
}
