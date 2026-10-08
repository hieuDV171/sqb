package com.frozenheart.backend.modules.search.controller;

import com.frozenheart.backend.core.dto.GlobalResponse;
import com.frozenheart.backend.modules.search.dto.*;
import com.frozenheart.backend.modules.search.service.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "25. Tìm kiếm Toàn văn (Search)", description = "APIs tìm kiếm toàn văn đa thực thể (Elasticsearch) và quản lý lịch sử tìm kiếm")
@RestController
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    @Operation(summary = "Tìm kiếm toàn cục đa thực thể", description = "Tìm kiếm toàn văn trên Elasticsearch đa chỉ mục (Bài viết, Người dùng, Câu hỏi, Phiên đóng góp, Môn học) có hỗ trợ phân trang con trỏ.")
    @ApiResponse(responseCode = "200", description = "Tìm kiếm thành công")
    @GetMapping("/search")
    public ResponseEntity<GlobalResponse<GlobalSearchResponseDto>> search(
            @Parameter(description = "Từ khóa tìm kiếm", example = "lập trình java") @RequestParam(required = false) String query,
            @Parameter(description = "Loại thực thể cần tìm kiếm (ALL, POST, USER, QUESTION, SESSION, SUBJECT)") @RequestParam(required = false, defaultValue = "ALL") SearchType type,
            @Parameter(description = "Vị trí bắt đầu tìm kiếm (from offset)") @RequestParam(required = false) Long after,
            @Parameter(description = "Số lượng bản ghi mỗi trang (tối đa 50)", example = "10") @RequestParam(required = false, defaultValue = "10") Integer limit
    ) {
        GlobalSearchResponseDto response = searchService.search(query, type, after, limit);
        return ResponseEntity.ok(GlobalResponse.success(response));
    }

    @Operation(summary = "Lấy lịch sử tìm kiếm cá nhân & Top từ khóa thịnh hành", description = "Truy vấn tối đa 20 từ khóa tìm kiếm gần nhất của người dùng hiện tại và top 10 từ khóa tìm kiếm phổ biến nhất toàn hệ thống từ Redis.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lấy lịch sử và từ khóa thịnh hành thành công"),
            @ApiResponse(responseCode = "401", description = "Chưa xác thực người dùng")
    })
    @GetMapping("/search/saved")
    public ResponseEntity<GlobalResponse<SavedAndTrendingSearchesResponseDto>> getSavedAndTrendingSearches() {
        SavedAndTrendingSearchesResponseDto response = searchService.getSavedAndTrendingSearches();
        return ResponseEntity.ok(GlobalResponse.success(response));
    }

    @Operation(summary = "Xóa một từ khóa khỏi lịch sử tìm kiếm cá nhân", description = "Xóa bản ghi từ khóa tìm kiếm đã lưu của người dùng theo ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Xóa từ khóa tìm kiếm thành công"),
            @ApiResponse(responseCode = "401", description = "Chưa xác thực người dùng")
    })
    @DeleteMapping("/search/saved/{id}")
    public ResponseEntity<GlobalResponse<DeleteSavedSearchResponseDto>> deleteSavedSearch(
            @Parameter(description = "ID bản ghi từ khóa tìm kiếm cần xóa", example = "15") @PathVariable Long id) {
        DeleteSavedSearchResponseDto response = searchService.deleteSavedSearch(id);
        return ResponseEntity.ok(GlobalResponse.success(response));
    }
}
