import { useQuery } from '@tanstack/react-query';
import { sessionService } from '../services/sessionService';

export function useEnrolledSubjects() {
  return useQuery({
    queryKey: ['enrolled-subjects'],
    queryFn: async () => {
      const res = await sessionService.getMyEnrolledSubjects();
      return res.data || [];
    },
    staleTime: 10 * 60 * 1000, // 10 minutes
  });
}
