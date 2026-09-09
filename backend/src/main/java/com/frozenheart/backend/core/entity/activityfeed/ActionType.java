package com.frozenheart.backend.core.entity.activityfeed;

public enum ActionType {
    // Social Posts
    CREATED_POST,
    PUBLISHED_LECTURE_VIDEO,
    // QUESTION_APPROVED_AUTO_POST,

    // Academic Sessions (Only when RESOLVED)
    SESSION_RESOLVED_APPROVED,

    // Gamification & Cosmetics
    EARNED_BADGE,
    REACHED_MILESTONE,
}
