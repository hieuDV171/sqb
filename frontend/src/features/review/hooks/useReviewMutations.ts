import { useMutation, useQueryClient } from '@tanstack/react-query';
import { toast } from '@/stores/useToastStore';
import { reviewService } from '../services/reviewService';
import type {
  ApproveQuestionsRequest, EditQuestionRequest, RejectQuestionsRequest,
} from '../types/review.types';

export function useReviewMutations(sessionId: number) {
  const queryClient = useQueryClient();
  const detailKey = ['reviewSessionDetail', sessionId] as const;
  const queueKey = ['reviewableSessions'] as const;

  const refreshReview = () => Promise.all([
    queryClient.invalidateQueries({ queryKey: queueKey }),
    queryClient.invalidateQueries({ queryKey: detailKey, exact: true }),
  ]);
  const handleDecisionError = (error: Error, fallback: string) => {
    toast.error(error.message || fallback);
    if ('code' in error && error.code === '2017') {
      return refreshReview();
    }
  };

  const approve = useMutation({
    mutationFn: (request: ApproveQuestionsRequest) => reviewService.approveQuestions(request),
    onSuccess: async (_, { questionIds }) => {
      await refreshReview();
      toast.success(`Đã phê duyệt ${questionIds.length} câu hỏi.`);
    },
    onError: (error) => handleDecisionError(error, 'Không thể phê duyệt câu hỏi. Hãy thử lại.'),
  });

  const reject = useMutation({
    mutationFn: (request: RejectQuestionsRequest) => reviewService.rejectQuestions(request),
    onSuccess: async () => {
      await refreshReview();
      toast.success('Đã lưu quyết định từ chối.');
    },
    onError: (error) => handleDecisionError(error, 'Không thể từ chối câu hỏi. Hãy thử lại.'),
  });

  const edit = useMutation({
    mutationFn: ({ questionId, data }: { questionId: number; data: EditQuestionRequest }) =>
      reviewService.editQuestion(questionId, data),
    onSuccess: async ({ data }) => {
      await refreshReview();
      toast.success(data?.status === 'APPROVED' ? 'Đã lưu và phê duyệt câu hỏi.' : 'Đã lưu câu hỏi.');
    },
    onError: (error) => handleDecisionError(error, 'Không thể lưu câu hỏi. Hãy thử lại.'),
  });

  const complete = useMutation({
    mutationFn: () => reviewService.completeSessionReview(sessionId),
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: queueKey }),
        queryClient.invalidateQueries({ queryKey: detailKey, exact: true, refetchType: 'none' }),
      ]);
      toast.success('Đã hoàn tất duyệt phiên.');
    },
    onError: () => toast.error('Không thể hoàn tất duyệt phiên. Hãy thử lại.'),
  });

  return { approve, reject, edit, complete };
}
