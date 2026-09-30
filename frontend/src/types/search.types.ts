/**
 * Search Module Types & Contracts
 * Tuân thủ quy ước tên ID tường minh
 */

export interface SavedSearchDto {
    savedSearchId: number;
    queryText: string;
    lastSearchedAt: string;
}

export interface DeleteSavedSearchResponseDto {
    savedSearchId: number;
}

export interface SavedAndTrendingSearchesResponseDto {
    savedSearches: SavedSearchDto[];
    trendingSearches: string[];
}

/**
 * TODO: HƯỚNG DẪN KỸ THUẬT OPTIMISTIC UI KHI XÓA TÌM KIẾM ĐÃ LƯU:
 * Endpoint: DELETE /search/saved/{id} -> trả về { savedSearchId: number }
 * 
 * Khi triển khai useMutation trong TanStack Query:
 * 1. onMutate: 
 *    - Ẩn ngay lập tức từ khóa khỏi UI (Zero Perceived Latency).
 *    - Lưu snapshot danh sách cũ vào context: const prev = queryClient.getQueryData(['savedSearches'])
 *    - Cập nhật cache: queryClient.setQueryData(['savedSearches'], old => filterOut(item.savedSearchId))
 * 2. onError:
 *    - Nếu mạng lag / timeout / lỗi server: ROLLBACK lại cache cũ từ context:
 *      queryClient.setQueryData(['savedSearches'], context.prev)
 *    - Hiển thị toast cảnh báo: toast.error("Không thể xóa từ khóa, vui lòng kiểm tra kết nối mạng!")
 * 3. onSettled:
 *    - queryClient.invalidateQueries(['savedSearches']) để đồng bộ trạng thái chắc chắn với server.
 */
