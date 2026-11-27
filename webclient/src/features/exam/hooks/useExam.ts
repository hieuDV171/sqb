import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { examService } from '@/features/exam';
import type {
  GenerateExamRequest,
  ExportExamParams,
  ExportQuestionsParams,
} from '@/features/exam';
import { toast } from '@/stores/useToastStore';

export const EXAM_KEYS = {
  all: ['exams'] as const,
  list: (subjectId?: number) => [...EXAM_KEYS.all, 'list', subjectId] as const,
  detail: (examId?: number) => [...EXAM_KEYS.all, 'detail', examId] as const,
  bankSummary: (subjectId?: number) => [...EXAM_KEYS.all, 'bank-summary', subjectId] as const,
  classes: (params?: { semesterId?: number; includeHistory?: boolean }) =>
    [...EXAM_KEYS.all, 'lecturer-classes', params] as const,
};

export function useExamBankSummary(subjectId?: number) {
  return useQuery({
    queryKey: EXAM_KEYS.bankSummary(subjectId),
    enabled: subjectId !== undefined,
    queryFn: async ({ signal }) => subjectId === undefined ? null : (await examService.getBankSummary(subjectId, signal)).data,
  });
}

export function useMyExams(params?: {
  subjectId?: number;
  after?: number;
  before?: number;
  limit?: number;
}) {
  return useQuery({
    queryKey: [...EXAM_KEYS.list(params?.subjectId), params],
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

export function useLecturerClasses(params?: {
  semesterId?: number;
  includeHistory?: boolean;
}) {
  return useQuery({
    queryKey: EXAM_KEYS.classes(params),
    queryFn: async () => {
      const res = await examService.getLecturerClasses(params);
      return res.data || [];
    },
  });
}

export function useGenerateExam() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: GenerateExamRequest) => examService.generateExam(data),
    onSuccess: () => {
      toast.success('Đã tạo đề thi.');
      void queryClient.invalidateQueries({ queryKey: [...EXAM_KEYS.all, 'list'] });
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
    onSuccess: downloadExport,
  });
}

export function useExportQuestions() {
  return useMutation({
    mutationFn: (params: ExportQuestionsParams) =>
      examService.exportQuestions(params),
    onSuccess: downloadExport,
  });
}

function downloadExport(response: { data: { downloadUrl: string } }) {
  if (!response.data?.downloadUrl) return;

  const link = document.createElement('a');
  link.href = response.data.downloadUrl;
  link.referrerPolicy = 'no-referrer';
  link.download = '';
  document.body.append(link);
  link.click();
  link.remove();
}
