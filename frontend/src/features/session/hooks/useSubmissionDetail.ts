import { useQuery } from '@tanstack/react-query';
import { sessionService } from '../services/sessionService';

export function useSubmissionDetail(sessionId: number | null | undefined) {
  return useQuery({
    queryKey: ['my-submission-detail', sessionId],
    queryFn: async () => {
      if (!sessionId) throw new Error('ID phiên không hợp lệ');
      const res = await sessionService.getMySubmissionDetail(sessionId);
      return res.data;
    },
    enabled: Boolean(sessionId),
    staleTime: 60 * 1000,
  });
}
