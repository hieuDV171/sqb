import { useQuery } from '@tanstack/react-query';
import { sessionService } from '@/features/session';

export function useEnrolledCourseClasses() {
  return useQuery({
    queryKey: ['enrolled-course-classes'],
    queryFn: async () => {
      const response = await sessionService.getMyEnrolledCourseClasses();
      return response.data || [];
    },
    staleTime: 10 * 60 * 1000,
  });
}
