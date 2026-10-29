import { axiosClient } from '@/api/axiosClient';
import type { GlobalResponse } from '@/types/response.types';
import type {
  AssignStudentsRequest,
  AssignStudentsResponse,
  ClassStudentResponse,
  CourseClassResponse,
  CreateCourseClassRequest,
  LecturerSummaryResponse,
} from '@/features/course-class';

export const courseClassService = {
  getActiveClasses: async (): Promise<GlobalResponse<CourseClassResponse[]>> =>
    (await axiosClient.get('/lecturer/course-classes')) as never,

  getMyClasses: async (includeHistory = true): Promise<GlobalResponse<CourseClassResponse[]>> =>
    (await axiosClient.get('/lecturer/course-classes/my-classes', {
      params: { includeHistory },
    })) as never,

  getDetail: async (courseClassId: number): Promise<GlobalResponse<CourseClassResponse>> =>
    (await axiosClient.get(`/lecturer/course-classes/${courseClassId}`)) as never,

  getStudents: async (courseClassId: number): Promise<GlobalResponse<ClassStudentResponse[]>> =>
    (await axiosClient.get(`/lecturer/course-classes/${courseClassId}/students`)) as never,

  getLecturers: async (): Promise<GlobalResponse<LecturerSummaryResponse[]>> =>
    (await axiosClient.get('/lecturer/course-classes/lecturers')) as never,

  create: async (request: CreateCourseClassRequest): Promise<GlobalResponse<CourseClassResponse>> =>
    (await axiosClient.post('/lecturer/course-classes', request)) as never,

  assignStudents: async (
    courseClassId: number,
    request: AssignStudentsRequest
  ): Promise<GlobalResponse<AssignStudentsResponse>> =>
    (await axiosClient.post(`/lecturer/course-classes/${courseClassId}/students`, request)) as never,
};
