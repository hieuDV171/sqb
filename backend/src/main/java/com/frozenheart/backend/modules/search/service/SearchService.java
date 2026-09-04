package com.frozenheart.backend.modules.search.service;

import com.frozenheart.backend.modules.search.dto.GlobalSearchResponseDto;
import com.frozenheart.backend.modules.search.dto.SavedAndTrendingSearchesResponseDto;
import com.frozenheart.backend.modules.search.dto.SearchScope;
import com.frozenheart.backend.modules.search.dto.SearchType;

public interface SearchService {

    /**
     * Tìm kiếm toàn cục đa thực thể trên Elasticsearch
     */
    GlobalSearchResponseDto search(
            String query,
            SearchType type,
            SearchScope scope,
            Long after,
            Integer limit
    );

    /**
     * Lấy lịch sử tìm kiếm cá nhân & Top thịnh hành trên Redis
     */
    SavedAndTrendingSearchesResponseDto getSavedAndTrendingSearches();

    /**
     * Xóa 1 từ khóa khỏi lịch sử tìm kiếm cá nhân
     */
    void deleteSavedSearch(Long id);

    /**
     * Lưu lịch sử tìm kiếm vào DB & tăng điểm Trending trên Redis
     */
    void recordSearchQuery(Long currentUserId, String query);
}
