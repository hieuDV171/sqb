import { useInfiniteQuery } from '@tanstack/react-query';
import { practiceService } from '../services/practiceService';
export const practiceCatalogQueryKey = ['practice-catalog'] as const;

export function usePracticeCatalog(subjectId: number | undefined, keyword: string) {
  return useInfiniteQuery({
    queryKey: [...practiceCatalogQueryKey, subjectId, keyword],
    initialPageParam: undefined as number | undefined,
    queryFn: async ({ pageParam }) => {
      const res = await practiceService.getPracticeCatalog({
        after: pageParam,
        subject_id: subjectId,
        q: keyword || undefined,
      });
      if (!res.data) throw new Error(res.message || 'Không thể tải kho luyện tập.');
      return res.data;
    },
    getNextPageParam: (page) => page.pagination.hasNext ? page.pagination.after ?? undefined : undefined,
    staleTime: 60_000,
  });
}
