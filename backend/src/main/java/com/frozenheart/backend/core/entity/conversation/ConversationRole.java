package com.frozenheart.backend.core.entity.conversation;

public enum ConversationRole {
    CHIEF, // Tù trưởng (Trưởng nhóm / Owner) - Duy nhất 1 người
    VILLAGE_ELDER, // Già làng (Phó nhóm / Co-Leader / Mod) - Có thể có nhiều người
    VILLAGER // Dân làng (Thành viên thông thường)
}
