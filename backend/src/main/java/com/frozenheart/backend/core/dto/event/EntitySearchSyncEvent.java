package com.frozenheart.backend.core.dto.event;

import java.util.List;

import lombok.Builder;

@Builder
public record EntitySearchSyncEvent(
    EntityType entityType,
    List<Long> entityIds,
    SyncAction action
) {
    public enum EntityType {
        USER, POST, SESSION, QUESTION, SUBJECT
    }

    public enum SyncAction {
        UPSERT, DELETE
    }

    public static EntitySearchSyncEvent upsert(EntityType type, Long id) {
        return new EntitySearchSyncEvent(type, List.of(id), SyncAction.UPSERT);
    }

    public static EntitySearchSyncEvent upsertBatch(EntityType type, List<Long> ids) {
        return new EntitySearchSyncEvent(type, ids, SyncAction.UPSERT);
    }

    public static EntitySearchSyncEvent delete(EntityType type, Long id) {
        return new EntitySearchSyncEvent(type, List.of(id), SyncAction.DELETE);
    }
}
