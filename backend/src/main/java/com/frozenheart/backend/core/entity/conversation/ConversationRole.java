package com.frozenheart.backend.core.entity.conversation;

public enum ConversationRole {
    LEADER, // Tù trưởng (Trưởng nhóm / Owner) - Duy nhất 1 người
    DEPUTY, // Già làng (Phó nhóm / Co-Leader / Mod) - Có thể có nhiều người
    MEMBER // Dân làng (Thành viên thông thường)
}
