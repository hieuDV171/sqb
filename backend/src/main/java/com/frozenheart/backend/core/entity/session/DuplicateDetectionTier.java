package com.frozenheart.backend.core.entity.session;

public enum DuplicateDetectionTier {
    RULE_BASED,
    TRIGRAM,
    P_HASH,
    TEXT_EMBEDDING,
    IMAGE_EMBEDDING
}
