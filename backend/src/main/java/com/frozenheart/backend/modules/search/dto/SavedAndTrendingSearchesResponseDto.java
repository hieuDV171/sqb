package com.frozenheart.backend.modules.search.dto;

import lombok.*;

import java.time.LocalDateTime;
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
        private Long id;
        private String queryText;
        private LocalDateTime lastSearchedAt;
    }
}
