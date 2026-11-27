import { useQuery } from '@tanstack/react-query';
import { adminService } from '@/services/adminService';

export const ADMIN_SEMESTERS_KEY = ['admin', 'semesters'] as const;

export function useAdminSemesters(enabled = true) {
  return useQuery({
    queryKey: ADMIN_SEMESTERS_KEY,
    queryFn: async () => (await adminService.getAllSemesters()).data,
    enabled,
  });
}
