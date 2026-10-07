import { axiosClient } from '@/api/axiosClient';
import type { GlobalResponse } from '@/types/response.types';
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
    return (await axiosClient.post<GlobalResponse<ExamDetailResponse>>(
      '/exams/generate',
      data
    )) as any;
  },

  /**
   * 21.2 Xem chi tiết đề thi
   * GET /api/v1/exams/{examId}
   */
  getExamDetail: async (
    examId: number
  ): Promise<GlobalResponse<ExamDetailResponse>> => {
    return (await axiosClient.get<GlobalResponse<ExamDetailResponse>>(
      `/exams/${examId}`
    )) as any;
  },

  /**
   * 21.3 Lấy danh sách đề thi của giảng viên
   * GET /api/v1/exams/my-exams
   */
  getMyExams: async (params?: {
    subjectId?: number;
    after?: number;
    limit?: number;
  }): Promise<GlobalResponse<ExamListResponse>> => {
    return (await axiosClient.get<GlobalResponse<ExamListResponse>>(
      '/exams/my-exams',
      {
        params: {
          subject_id: params?.subjectId,
          after: params?.after,
          limit: params?.limit ?? 10,
        },
      }
    )) as any;
  },

  /**
   * 21.4 Xuất file đề thi (PDF / Excel)
   * GET /api/v1/exams/{examId}/export
   */
  exportExam: async (
    examId: number,
    params?: ExportExamParams
  ): Promise<GlobalResponse<ExportResponse>> => {
    return (await axiosClient.get<GlobalResponse<ExportResponse>>(
      `/exams/${examId}/export`,
      {
        params: {
          format: params?.format ?? 'pdf',
          include_answer_key: params?.includeAnswerKey ?? false,
          paper_size: params?.paperSize ?? 'A4',
        },
      }
    )) as any;
  },

  /**
   * 22.1 Giảng viên xuất toàn bộ câu hỏi môn học
   * GET /api/v1/lecturer/questions/export
   */
  exportQuestions: async (
    params: ExportQuestionsParams
  ): Promise<GlobalResponse<ExportResponse>> => {
    return (await axiosClient.get<GlobalResponse<ExportResponse>>(
      '/lecturer/questions/export',
      {
        params: {
          subject_id: params.subjectId,
          format: params.format ?? 'pdf',
          include_answer: params.includeAnswer ?? true,
        },
      }
    )) as any;
  },

  /**
   * Lấy danh sách lớp học phần của giảng viên để gán đề thi
   * GET /api/v1/lecturer/course-classes/my-classes
   */
  getLecturerClasses: async (): Promise<GlobalResponse<any[]>> => {
    return (await axiosClient.get<GlobalResponse<any[]>>(
      '/lecturer/course-classes/my-classes'
    )) as any;
  },
};
