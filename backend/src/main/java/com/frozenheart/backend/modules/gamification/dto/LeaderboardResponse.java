package com.frozenheart.backend.modules.gamification.dto;

import com.frozenheart.backend.core.dto.pagination.CursorPaginationDto;
import lombok.Builder;
import java.util.List;

@Builder
public record LeaderboardResponse(
        List<LeaderboardEntryDto> entries,
        MyRankDto myRank,
        CursorPaginationDto pagination
) {}
