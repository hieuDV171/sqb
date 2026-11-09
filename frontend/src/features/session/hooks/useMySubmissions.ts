import { useInfiniteQuery } from '@tanstack/react-query';
import { sessionService } from '../services/sessionService';
import type { SessionStatus } from '../types/session.types';

interface UseMySubmissionsOptions {
  subjectId?: number;
  status?: SessionStatus;
  limit?: number;
  enabled?: boolean;
}

export function useMySubmissions({
  subjectId,
  status,
  limit = 10,
  enabled = true,
}: UseMySubmissionsOptions = {}) {
  return useInfiniteQuery({
    enabled,
    queryKey: ['my-submissions', { subjectId, status, limit }],
    queryFn: async ({ pageParam }) => {
      const response = await sessionService.getMySubmissions({
        after: pageParam,
        limit,
        subject_id: subjectId,
        status,
      });
      return response.data;
    },
    initialPageParam: undefined as number | undefined,
    getNextPageParam: (lastPage, _pages, _lastPageParam, pageParams) => {
      const next = lastPage?.pagination?.after;
      if (lastPage?.pagination?.hasNext && typeof next === 'number' && !pageParams.includes(next)) {
        return next;
      }
      return undefined;
    },
    staleTime: 60 * 1000, // 1 minute
  });
}
