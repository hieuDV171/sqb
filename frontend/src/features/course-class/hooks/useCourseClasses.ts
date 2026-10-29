import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { toast } from '@/stores/useToastStore';
import { courseClassService } from '@/features/course-class';
import type { AssignStudentsRequest, CreateCourseClassRequest } from '@/features/course-class';

export const COURSE_CLASS_KEYS = {
  all: ['course-classes'] as const,
  list: (isAdmin: boolean, includeHistory: boolean) =>
    [...COURSE_CLASS_KEYS.all, 'list', isAdmin, includeHistory] as const,
  detail: (courseClassId: number) => [...COURSE_CLASS_KEYS.all, 'detail', courseClassId] as const,
  students: (courseClassId: number) => [...COURSE_CLASS_KEYS.all, 'students', courseClassId] as const,
  lecturers: () => [...COURSE_CLASS_KEYS.all, 'lecturers'] as const,
};

export function useCourseClasses(isAdmin: boolean, includeHistory = true) {
  return useQuery({
    queryKey: COURSE_CLASS_KEYS.list(isAdmin, includeHistory),
    queryFn: async () => {
      const response = isAdmin
        ? await courseClassService.getActiveClasses()
        : await courseClassService.getMyClasses(includeHistory);
      return response.data ?? [];
    },
    staleTime: 60_000,
  });
}

export function useCourseClassDetail(courseClassId: number) {
  return useQuery({
    queryKey: COURSE_CLASS_KEYS.detail(courseClassId),
    queryFn: async () => (await courseClassService.getDetail(courseClassId)).data,
    enabled: Number.isFinite(courseClassId) && courseClassId > 0,
  });
}

export function useClassStudents(courseClassId: number) {
  return useQuery({
    queryKey: COURSE_CLASS_KEYS.students(courseClassId),
    queryFn: async () => (await courseClassService.getStudents(courseClassId)).data ?? [],
    enabled: Number.isFinite(courseClassId) && courseClassId > 0,
  });
}

export function useLecturers(enabled: boolean) {
  return useQuery({
    queryKey: COURSE_CLASS_KEYS.lecturers(),
    queryFn: async () => (await courseClassService.getLecturers()).data ?? [],
    enabled,
    staleTime: 5 * 60_000,
  });
}

export function useCreateCourseClass() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (request: CreateCourseClassRequest) => courseClassService.create(request),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: COURSE_CLASS_KEYS.all });
      toast.success('Đã tạo lớp học phần trong học kỳ hiện tại');
    },
  });
}

export function useAssignStudents(courseClassId: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (request: AssignStudentsRequest) =>
      courseClassService.assignStudents(courseClassId, request),
    onSuccess: async (response) => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: COURSE_CLASS_KEYS.students(courseClassId) }),
        queryClient.invalidateQueries({ queryKey: COURSE_CLASS_KEYS.detail(courseClassId) }),
        queryClient.invalidateQueries({ queryKey: COURSE_CLASS_KEYS.all }),
      ]);
      toast.success(`Đã thêm ${response.data.totalEnrolled} sinh viên vào lớp`);
    },
  });
}
