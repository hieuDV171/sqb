import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { searchService } from '../services/searchService';
import type { SearchType } from '../types/search.types';

export const SEARCH_KEYS = {
  all: ['search'] as const,
  query: (q?: string, type?: SearchType, after?: number, limit?: number) =>
    [...SEARCH_KEYS.all, 'query', q, type, after, limit] as const,
  savedAndTrending: () => [...SEARCH_KEYS.all, 'saved-trending'] as const,
};

export function useGlobalSearch(params: {
  query?: string;
  type?: SearchType;
  after?: number;
  limit?: number;
  enabled?: boolean;
}) {
  return useQuery({
    queryKey: SEARCH_KEYS.query(
      params.query,
      params.type,
      params.after,
      params.limit
    ),
    queryFn: async () => {
      const res = await searchService.search(params);
      return res.data;
    },
    enabled: params.enabled !== undefined ? params.enabled : !!params.query?.trim(),
  });
}

export function useSavedAndTrendingSearches(enabled = true) {
  return useQuery({
    queryKey: SEARCH_KEYS.savedAndTrending(),
    queryFn: async () => {
      const res = await searchService.getSavedAndTrendingSearches();
      return res.data;
    },
    enabled,
    staleTime: 60000,
  });
}

export function useDeleteSavedSearch() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: number) => searchService.deleteSavedSearch(id),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: SEARCH_KEYS.savedAndTrending(),
      });
    },
  });
}
