import { queryOptions } from '@tanstack/react-query';
import { sessionService } from './sessionService';
import { courseClassService } from '@/features/course-class/services/courseClassService';
import type { UserRole } from '@/stores/useAuthStore';
import type { SubjectResponse } from '@/features/session';

export type SubjectScope = 'enrolled' | 'catalog' | 'publishing';

export function subjectOptionsQuery(userId: number, role: UserRole, scope: SubjectScope) {
  return queryOptions({
    queryKey: ['subject-options', userId, role, scope] as const,
    staleTime: 2 * 60 * 1000,
    queryFn: async ({ signal }): Promise<SubjectResponse[]> => {
      if (scope === 'catalog') return (await sessionService.getAllSubjects(signal)).data ?? [];

      const enrolled = (await sessionService.getMyEnrolledSubjects(signal)).data ?? [];
      signal.throwIfAborted();
      if (scope === 'enrolled' || enrolled.length > 0) return enrolled;

      // Preserve the publishing form's existing fallback order and current classes.
      if (role === 'LECTURER') {
        const classes = (await courseClassService.getMyClasses(false, signal)).data ?? [];
        return [...new Map(classes.filter(item => item.subjectId).map(item => [item.subjectId, {
          subjectId: item.subjectId, code: item.subjectCode || '', name: item.subjectName || '',
        }])).values()];
      }
      if (role === 'ADMIN') return (await sessionService.getAllSubjects(signal)).data ?? [];
      return [];
    },
  });
}
