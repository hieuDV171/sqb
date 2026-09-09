package com.frozenheart.backend.modules.activityfeed.dto;

import com.frozenheart.backend.core.entity.activityfeed.ActionType;
import com.frozenheart.backend.modules.post.dto.AuthorDto;
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
public class ActivityFeedItemDto {
    private Long feedId;
    private String targetType;
    private Long targetId;
    private ActionType actionType;
    private AuthorDto actor;
    private Object content;

    private Instant createdAt;

    private Integer weight;
}
