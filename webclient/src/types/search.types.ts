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


