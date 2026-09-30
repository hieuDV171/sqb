import { useInfiniteQuery } from '@tanstack/react-query';
import { sessionService } from '../services/sessionService';
import type { SessionStatus } from '../types/session.types';

interface UseMySubmissionsOptions {
  subjectId?: number;
  status?: SessionStatus;
  limit?: number;
}

export function useMySubmissions({
  subjectId,
  status,
  limit = 10,
}: UseMySubmissionsOptions = {}) {
  return useInfiniteQuery({
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
    getNextPageParam: (lastPage) => {
      if (lastPage?.pagination?.hasNext && lastPage.pagination.after != null) {
        return lastPage.pagination.after;
      }
      return undefined;
    },
    staleTime: 60 * 1000, // 1 minute
  });
}
