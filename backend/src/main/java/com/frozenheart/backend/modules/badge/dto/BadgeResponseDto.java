package com.frozenheart.backend.modules.badge.dto;

import com.frozenheart.backend.core.entity.badge.BadgeCriteria;
import com.frozenheart.backend.core.entity.badge.BadgeTier;
import com.frozenheart.backend.core.entity.badge.BadgeTriggerEvent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BadgeResponseDto {

    private Long badgeId;
    private String name;
    private String description;
    private BadgeTier badgeTier;
    private BadgeTriggerEvent badgeTriggerEvent;
    private String iconUrl;
    private BadgeCriteria criteria;
    private Integer totalEarnedUsers;
    private Boolean active;
    private Instant createdAt;
}
