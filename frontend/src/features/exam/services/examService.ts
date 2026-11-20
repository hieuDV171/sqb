import { axiosClient } from '@/api/axiosClient';
import type { GlobalResponse } from '@/types/response.types';
import type { CourseClassResponse } from '@/features/course-class/types/courseClass.types';
import type {
  GenerateExamRequest,
  ExamDetailResponse,
  ExamListResponse,
  ExportResponse,
  ExportExamParams,
  ExportQuestionsParams,
} from '../types/exam.types';

export const examService = {
  /**
   * 21.1 Tự động sinh đề thi từ ngân hàng câu hỏi
   * POST /api/v1/exams/generate
   */
  generateExam: async (
    data: GenerateExamRequest
  ): Promise<GlobalResponse<ExamDetailResponse>> => {
    return axiosClient.post<unknown, GlobalResponse<ExamDetailResponse>>(
      '/exams/generate',
      data,
      { skipToast: true },
    );
  },

  /**
   * 21.2 Xem chi tiết đề thi
   * GET /api/v1/exams/{examId}
   */
  getExamDetail: async (
    examId: number
  ): Promise<GlobalResponse<ExamDetailResponse>> => {
    return axiosClient.get<unknown, GlobalResponse<ExamDetailResponse>>(`/exams/${examId}`);
  },

  /**
   * 21.3 Lấy danh sách đề thi của giảng viên
   * GET /api/v1/exams/my-exams
   */
  getMyExams: async (params?: {
    subjectId?: number;
    after?: number;
    before?: number;
    limit?: number;
  }): Promise<GlobalResponse<ExamListResponse>> => {
    return axiosClient.get<unknown, GlobalResponse<ExamListResponse>>(
      '/exams/my-exams',
      {
        params: {
          subject_id: params?.subjectId,
          after: params?.after,
          before: params?.before,
          limit: params?.limit ?? 10,
        },
      }
    );
  },

  /**
   * 21.4 Xuất file đề thi (PDF / Excel)
   * GET /api/v1/exams/{examId}/export
   */
  exportExam: async (
    examId: number,
    params?: ExportExamParams
  ): Promise<GlobalResponse<ExportResponse>> => {
    return axiosClient.get<unknown, GlobalResponse<ExportResponse>>(
      `/exams/${examId}/export`,
      {
        skipToast: true,
        params: {
          format: params?.format ?? 'pdf',
          include_answer_key: params?.includeAnswerKey ?? false,
        },
      }
    );
  },

  /**
   * 22.1 Giảng viên xuất toàn bộ câu hỏi môn học
   * GET /api/v1/lecturer/questions/export
   */
  exportQuestions: async (
    params: ExportQuestionsParams
  ): Promise<GlobalResponse<ExportResponse>> => {
    return axiosClient.get<unknown, GlobalResponse<ExportResponse>>(
      '/lecturer/questions/export',
      {
        skipToast: true,
        params: {
          subject_id: params.subjectId,
          format: params.format ?? 'pdf',
          include_answer: params.includeAnswer ?? true,
        },
      }
    );
  },

  /**
   * Lấy danh sách lớp học phần của giảng viên để gán đề thi
   * GET /api/v1/lecturer/course-classes/my-classes
   */
  getLecturerClasses: async (params?: {
    semesterId?: number;
    includeHistory?: boolean;
  }): Promise<GlobalResponse<CourseClassResponse[]>> => {
    return axiosClient.get<unknown, GlobalResponse<CourseClassResponse[]>>(
      '/lecturer/course-classes/my-classes',
      {
        params: {
          semester_id: params?.semesterId,
          include_history: params?.includeHistory,
        },
      }
    );
  },
};
