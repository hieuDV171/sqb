package com.frozenheart.backend.modules.search.dto;

import lombok.*;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SavedAndTrendingSearchesResponseDto {

    private List<SavedSearchDto> savedSearches;
    private List<String> trendingSearches;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SavedSearchDto {
        private Long savedSearchId;
        private String queryText;
        private Instant lastSearchedAt;
    }
}
