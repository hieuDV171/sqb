package com.frozenheart.backend.modules.search.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.Instant;
import java.util.List;

@Schema(description = "Lịch sử tìm kiếm cá nhân và danh sách từ khóa thịnh hành")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SavedAndTrendingSearchesResponseDto {

    @Schema(description = "Danh sách các từ khóa tìm kiếm gần đây của người dùng (tối đa 20)")
    private List<SavedSearchDto> savedSearches;

    @Schema(description = "Top 10 từ khóa tìm kiếm thịnh hành nhất toàn hệ thống", example = "[\"OOP\", \"Java Core\", \"Spring Boot\", \"Giải thuật Dijkstra\"]")
    private List<String> trendingSearches;

    @Schema(description = "Thông tin từ khóa đã lưu trong lịch sử")
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SavedSearchDto {

        @Schema(description = "ID từ khóa tìm kiếm", example = "12")
        private Long savedSearchId;

        @Schema(description = "Nội dung từ khóa tìm kiếm", example = "Cấu trúc dữ liệu")
        private String queryText;

        @Schema(description = "Thời điểm tìm kiếm gần nhất", example = "2026-10-06T11:00:00Z")
        private Instant lastSearchedAt;
    }
}
