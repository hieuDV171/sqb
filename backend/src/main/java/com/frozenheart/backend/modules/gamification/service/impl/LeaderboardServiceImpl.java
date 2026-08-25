package com.frozenheart.backend.modules.gamification.service.impl;

import com.frozenheart.backend.core.constant.Time;
import com.frozenheart.backend.core.dto.event.PointAddedEvent;
import com.frozenheart.backend.core.entity.session.Semester;
import com.frozenheart.backend.core.util.RedisKeyUtil;
import com.frozenheart.backend.modules.gamification.dto.LeaderboardAggregation;
import com.frozenheart.backend.modules.gamification.dto.LeaderboardPeriod;
import com.frozenheart.backend.modules.gamification.repository.PointHistoryRepository;
import com.frozenheart.backend.modules.gamification.service.LeaderboardService;
import com.frozenheart.backend.modules.session.component.CurrentSemesterHolder;
import org.springframework.data.redis.core.*;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class LeaderboardServiceImpl implements LeaderboardService {

    private final RedisTemplate<String, String> redisTemplate;
    private final DefaultRedisScript<Double> updateScoreScript;
    private final PointHistoryRepository pointHistoryRepository;

    private final Semester currentSemester;

//        ⚠️ Lưu ý quan trọng về kiến trúc Redis (Cluster Mode)
//    Phương pháp gom Key vào Lua Script này hoạt động hoàn hảo trên
//    Redis Standalone hoặc Redis Sentinel.
//
//    Tuy nhiên, nếu hệ thống đang chạy Redis Cluster (chia nhỏ dữ liệu ra nhiều node),
//    Redis sẽ báo lỗi CROSSSLOT Keys in request don't hash to the same slot khi chạy đoạn mã Lua.
//     Lý do là leaderboard:alltime và leaderboard:semester:1 có thể bị phân mảnh nằm ở 2 server vật lý khác nhau,
//     khiến Lua script không thể khóa cả 2 cùng lúc.
//
//    Nếu bạn đang dùng Redis Cluster, bạn phải quay lại sử dụng cơ chế Pipeline thuần túy của Spring Data Redis
//    bằng SessionCallback

    public LeaderboardServiceImpl(RedisTemplate<String, String> redisTemplate, CurrentSemesterHolder currentSemesterHolder, PointHistoryRepository pointHistoryRepository) {
        this.redisTemplate = redisTemplate;
        this.pointHistoryRepository = pointHistoryRepository;

        // Khởi tạo Lua Script
        String script =
                """                
                        -- Lấy thời gian 1 lần duy nhất để dùng chung cho mọi bảng xếp hạng
                        local server_time = redis.call('TIME')
                        local current_micro = tonumber(server_time[1]) * 1000000 + tonumber(server_time[2])
                        local max_time = 4096051200000000
                        local fraction = (max_time - current_micro) / max_time
                        
                        local added = tonumber(ARGV[2])
                        
                        -- Duyệt qua tất cả các KEYS được truyền vào từ Java
                        for i = 1, #KEYS do
                            local current_score = redis.call('ZSCORE', KEYS[i], ARGV[1])
                            local scaled = 0
                        
                            if current_score then
                                scaled = math.floor(tonumber(current_score))
                            end
                        
                            local new_score = scaled + added + fraction
                            redis.call('ZADD', KEYS[i], new_score, ARGV[1])
                        end
                        
                        return 1""";

        this.updateScoreScript = new DefaultRedisScript<>(script, Double.class);
        this.currentSemester = currentSemesterHolder.getCurrentSemester();
    }

    @Override
    public void updateLeaderboard(Long userId, double addedPoints, List<String> leaderboardKeys) {
        if (leaderboardKeys == null || leaderboardKeys.isEmpty()) {
            return;
        }

        String member = String.valueOf(userId);
        long scaledAddedPoints = Math.round(addedPoints * 100.0);

        // Gửi toàn bộ danh sách Keys vào Lua Script trong 1 lần thực thi
        redisTemplate.execute(
                updateScoreScript,
                leaderboardKeys,                           // Danh sách KEYS
                member,                                    // ARGV[1]
                String.valueOf(scaledAddedPoints)          // ARGV[2]
        );
    }

    @Transactional(readOnly = true)
    @Override
    public void rebuildLeaderboard(LeaderboardPeriod period, Long subjectId) {

        Long semesterId = currentSemester.getId();
        String redisKey = RedisKeyUtil.buildLeaderboardKey(period, semesterId, subjectId);

        rebuildLeaderboard(subjectId, semesterId, redisKey);

    }

    @Override
    public void rebuildLeaderboard(Long subjectId, Long semesterId, String leaderboardKey) {

        // Xóa trắng BXH cũ trên Redis để tránh rác
        redisTemplate.delete(leaderboardKey);

        // Lấy dữ liệu đã được gom nhóm tổng sẵn từ PostgreSQL
        List<LeaderboardAggregation> dbData = pointHistoryRepository.getAggregatedPointsForRebuild(subjectId, semesterId);

        if (dbData.isEmpty()) return;

        // Chuẩn bị danh sách Insert hàng loạt
        Set<ZSetOperations.TypedTuple<String>> tuplesToInsert = new HashSet<>();

        for (LeaderboardAggregation row : dbData) {
            String member = String.valueOf(row.getUserId());

            // Xử lý điểm
            long scaledPoints = Math.round(row.getTotalPoints() * 100.0);

            // TODO
            // Nên lấy báo Zone mặc định làm hằng số nếu muốn kết quả đồng bộ ở mọi zone
            // Hiện tại, systemDefault() sẽ cho kết quả khác nhau ở các zone khác nhau.

            // Chuyển LocalDateTime sang Milliseconds
            long lastEventMs = row.getLastEventTime()
                    .atZone(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli();

            // Tính phân số thời gian
            double timeFraction = (double) (Time.MAX_TIME_MS - lastEventMs) / (double) Time.MAX_TIME_MS;

            // Điểm ZSET cuối cùng
            double zScore = scaledPoints + timeFraction;

            // Thêm vào Set
            tuplesToInsert.add(new DefaultTypedTuple<>(member, zScore));

        }

        // Ghi toàn bộ lên Redis bằng 1 LỆNH DUY NHẤT
        redisTemplate.opsForZSet().add(leaderboardKey, tuplesToInsert);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    protected void handlePointAddedEvent(PointAddedEvent event) {
        Long userId = event.userId();
        double points = event.points();
        Long currentSemesterId = currentSemester.getId();

        List<String> targetKeys = new ArrayList<>();

        targetKeys.add(RedisKeyUtil.buildLeaderboardKey(LeaderboardPeriod.ALL_TIME, null, null));
        targetKeys.add(RedisKeyUtil.buildLeaderboardKey(LeaderboardPeriod.SEMESTER, currentSemesterId, null));
        if (event.subjectId() != null) {
            targetKeys.add(RedisKeyUtil.buildLeaderboardKey(LeaderboardPeriod.SUBJECT, currentSemesterId, event.subjectId()));
        }

        // Giao tiếp với Redis ĐÚNG 1 LẦN DUY NHẤT
        updateLeaderboard(userId, points, targetKeys);

    }

    @Override
    public void scheduleOldSemesterCleanup(Long oldSemesterId) {
    // Pattern tìm kiếm: "leaderboard:semester:{oldSemesterId}*"
    // Nó sẽ bao trùm cả BXH Semester và tất cả BXH Subject của kỳ đó
    String pattern = "leaderboard:semester:" + oldSemesterId + "*";

    // BẮT BUỘC dùng SCAN thay vì KEYS để không làm treo Redis
    ScanOptions options = ScanOptions.scanOptions().match(pattern).count(100).build();

    redisTemplate.execute((RedisCallback<Void>) connection -> {

        try (Cursor<byte[]> cursor = connection.keyCommands().scan(options)) {

            while (cursor.hasNext()) {
                byte[] key = cursor.next();

                // 30 ngày = 30 x 24 x 60 x 60 = 2.592.000 giây
                connection.keyCommands().expire(key, 2592000L);
            }

        }
        return null;
    });
}

}
