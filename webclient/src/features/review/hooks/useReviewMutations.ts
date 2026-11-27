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
  const handleDecisionError = (error: Error) => {
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
    onError: handleDecisionError,
  });

  const reject = useMutation({
    mutationFn: (request: RejectQuestionsRequest) => reviewService.rejectQuestions(request),
    onSuccess: async () => {
      await refreshReview();
      toast.success('Đã lưu quyết định từ chối.');
    },
    onError: handleDecisionError,
  });

  const edit = useMutation({
    mutationFn: ({ questionId, data }: { questionId: number; data: EditQuestionRequest }) =>
      reviewService.editQuestion(questionId, data),
    onSuccess: async ({ data }) => {
      await refreshReview();
      toast.success(data?.status === 'APPROVED' ? 'Đã lưu và phê duyệt câu hỏi.' : 'Đã lưu câu hỏi.');
    },
    onError: handleDecisionError,
  });

  const retryDuplicateCheck = useMutation({
    mutationFn: reviewService.retryDuplicateCheck,
    onSuccess: async () => {
      await refreshReview();
      toast.success('Đã yêu cầu dò trùng lại.');
    },
    onError: handleDecisionError,
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
  });

  return { approve, reject, edit, complete, retryDuplicateCheck };
}
