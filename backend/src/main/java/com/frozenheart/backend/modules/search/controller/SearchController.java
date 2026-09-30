package com.frozenheart.backend.modules.search.controller;

import com.frozenheart.backend.core.dto.GlobalResponse;
import com.frozenheart.backend.modules.search.dto.*;
import com.frozenheart.backend.modules.search.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    /**
     * API 5.3.5.1: Tìm kiếm toàn cục đa thực thể
     * GET /search?query=...&type=ALL&after=...&limit=10
     */
    @GetMapping("/search")
    public ResponseEntity<GlobalResponse<GlobalSearchResponseDto>> search(
            @RequestParam(required = false) String query,
            @RequestParam(required = false, defaultValue = "ALL") SearchType type,
            @RequestParam(required = false) Long after,
            @RequestParam(required = false, defaultValue = "10") Integer limit
    ) {
        GlobalSearchResponseDto response = searchService.search(query, type, after, limit);
        return ResponseEntity.ok(GlobalResponse.success(response));
    }

    /**
     * API 5.3.5.2: Lấy lịch sử tìm kiếm cá nhân & Top thịnh hành
     * GET /search/saved
     */
    @GetMapping("/search/saved")
    public ResponseEntity<GlobalResponse<SavedAndTrendingSearchesResponseDto>> getSavedAndTrendingSearches() {
        SavedAndTrendingSearchesResponseDto response = searchService.getSavedAndTrendingSearches();
        return ResponseEntity.ok(GlobalResponse.success(response));
    }

    /**
     * API 5.3.5.3: Xóa 1 từ khóa khỏi lịch sử tìm kiếm cá nhân
     * DELETE /search/saved/{id}
     */
    @DeleteMapping("/search/saved/{id}")
    public ResponseEntity<GlobalResponse<DeleteSavedSearchResponseDto>> deleteSavedSearch(@PathVariable Long id) {
        DeleteSavedSearchResponseDto response = searchService.deleteSavedSearch(id);
        return ResponseEntity.ok(GlobalResponse.success(response));
    }
}
