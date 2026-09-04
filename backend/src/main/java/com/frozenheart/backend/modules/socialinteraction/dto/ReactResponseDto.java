package com.frozenheart.backend.modules.socialinteraction.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReactResponseDto {
    private String myReaction;
    private Map<String, Integer> reactionCounts;
    private int totalCount;
}
