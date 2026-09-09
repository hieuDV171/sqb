package com.frozenheart.backend.core.util;

import com.frozenheart.backend.core.constant.ResponseCode;
import com.frozenheart.backend.core.exception.AppException;
import com.frozenheart.backend.modules.gamification.dto.LeaderboardPeriod;

public final class RedisKeyUtil {
    private RedisKeyUtil() {
    }

    public static String buildLeaderboardKey(LeaderboardPeriod period, Long semesterId, Long subjectId) {
        StringBuilder key = new StringBuilder("leaderboard:");

        switch (period) {
            case SEMESTER -> {
                if (semesterId == null)
                    throw new AppException(ResponseCode.MISSING_REQUIRED_PARAMETER, "Cần semesterId cho BXH SEMESTER");
                key.append(LeaderboardPeriod.SEMESTER.getRedisKey())
                        .append(":")
                        .append(semesterId);
            }
            case SUBJECT -> {
                if (semesterId == null) {
                    throw new AppException(ResponseCode.MISSING_REQUIRED_PARAMETER, "Cần semesterId cho BXH SUBJECT");
                }

                if (subjectId == null) {
                    throw new AppException(ResponseCode.MISSING_REQUIRED_PARAMETER, "Cần subjectId cho BXH SUBJECT");
                }

                key.append(LeaderboardPeriod.SEMESTER.getRedisKey())
                        .append(":")
                        .append(semesterId)
                        .append(":")
                        .append(LeaderboardPeriod.SUBJECT.getRedisKey())
                        .append(":")
                        .append(subjectId);
            }
        }
        return key.toString();
    }
}
