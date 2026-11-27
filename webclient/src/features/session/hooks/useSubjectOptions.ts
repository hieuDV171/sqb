import { useQuery } from '@tanstack/react-query';
import { useAuthStore } from '@/stores/useAuthStore';
import { subjectOptionsQuery, type SubjectScope } from '../services/subjectOptions';

export function useSubjectOptions(scope: SubjectScope) {
  const user = useAuthStore(state => state.user);
  const role = user?.role ?? 'STUDENT';
  const permitted = scope === 'enrolled' || role === 'LECTURER' || role === 'ADMIN';
  return useQuery({
    ...subjectOptionsQuery(user?.id ?? 0, role, scope),
    enabled: Boolean(user) && permitted,
  });
}
