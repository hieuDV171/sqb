import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { examService } from '../services/examService';
import type {
  GenerateExamRequest,
  ExportExamParams,
  ExportQuestionsParams,
} from '../types/exam.types';
import { toast } from '@/stores/useToastStore';

export const EXAM_KEYS = {
  all: ['exams'] as const,
  list: (subjectId?: number) => [...EXAM_KEYS.all, 'list', subjectId] as const,
  detail: (examId?: number) => [...EXAM_KEYS.all, 'detail', examId] as const,
  classes: () => [...EXAM_KEYS.all, 'lecturer-classes'] as const,
};

export function useMyExams(params?: {
  subjectId?: number;
  after?: number;
  limit?: number;
}) {
  return useQuery({
    queryKey: EXAM_KEYS.list(params?.subjectId),
    queryFn: async () => {
      const res = await examService.getMyExams(params);
      return res.data;
    },
  });
}

export function useExamDetail(examId?: number) {
  return useQuery({
    queryKey: EXAM_KEYS.detail(examId),
    queryFn: async () => {
      if (!examId) return null;
      const res = await examService.getExamDetail(examId);
      return res.data;
    },
    enabled: !!examId,
  });
}

export function useLecturerClasses() {
  return useQuery({
    queryKey: EXAM_KEYS.classes(),
    queryFn: async () => {
      const res = await examService.getLecturerClasses();
      return res.data || [];
    },
  });
}

export function useGenerateExam() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: GenerateExamRequest) => examService.generateExam(data),
    onSuccess: () => {
      toast.success('Sinh đề thi ngẫu nhiên thành công!');
      queryClient.invalidateQueries({ queryKey: EXAM_KEYS.all });
    },
    onError: (err: any) => {
      const msg =
        err.response?.data?.message ||
        'Không thể sinh đề thi. Vui lòng kiểm tra số lượng câu hỏi trong ngân hàng đề!';
      toast.error(msg);
    },
  });
}

export function useExportExam() {
  return useMutation({
    mutationFn: ({
      examId,
      params,
    }: {
      examId: number;
      params?: ExportExamParams;
    }) => examService.exportExam(examId, params),
    onSuccess: (res) => {
      if (res.data?.downloadUrl) {
        toast.success('Tạo liên kết tải đề thi thành công!');
        window.open(res.data.downloadUrl, '_blank');
      } else {
        toast.error('Không tìm thấy đường dẫn tải về.');
      }
    },
    onError: (err: any) => {
      toast.error(err.response?.data?.message || 'Lỗi khi xuất file đề thi.');
    },
  });
}

export function useExportQuestions() {
  return useMutation({
    mutationFn: (params: ExportQuestionsParams) =>
      examService.exportQuestions(params),
    onSuccess: (res) => {
      if (res.data?.downloadUrl) {
        toast.success('Xuất ngân hàng câu hỏi thành công!');
        window.open(res.data.downloadUrl, '_blank');
      } else {
        toast.error('Không tìm thấy đường dẫn tải về.');
      }
    },
    onError: (err: any) => {
      toast.error(
        err.response?.data?.message || 'Lỗi khi xuất ngân hàng câu hỏi.'
      );
    },
  });
}
