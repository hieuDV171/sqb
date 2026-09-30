import { useMutation, useQueryClient } from '@tanstack/react-query';
import { reviewService } from '../services/reviewService';
import { toast } from '@/stores/useToastStore';
import type {
  ApproveQuestionsRequest,
  RejectQuestionsRequest,
  EditQuestionRequest,
} from '../types/review.types';

export const useReviewMutations = (sessionId?: number) => {
  const queryClient = useQueryClient();

  const invalidateReviewQueries = () => {
    queryClient.invalidateQueries({ queryKey: ['pendingSessions'] });
    if (sessionId) {
      queryClient.invalidateQueries({ queryKey: ['reviewSessionDetail', sessionId] });
    }
  };

  // 16.3 Approve questions
  const approveMutation = useMutation({
    mutationFn: (data: ApproveQuestionsRequest) => reviewService.approveQuestions(data),
    onSuccess: (res) => {
      const pts = res.data?.pointsEarned || 0;
      toast.success(`Đã phê duyệt câu hỏi thành công! (+${pts} điểm vào ngân hàng)`);
      invalidateReviewQueries();
    },
    onError: (err: any) => {
      toast.error(err?.response?.data?.message || err?.message || 'Lỗi khi phê duyệt câu hỏi');
    },
  });

  // 16.4 Reject questions
  const rejectMutation = useMutation({
    mutationFn: (data: RejectQuestionsRequest) => reviewService.rejectQuestions(data),
    onSuccess: () => {
      toast.success('Đã từ chối câu hỏi thành công');
      invalidateReviewQueries();
    },
    onError: (err: any) => {
      toast.error(err?.response?.data?.message || err?.message || 'Lỗi khi từ chối câu hỏi');
    },
  });

  // 16.5 Edit question
  const editQuestionMutation = useMutation({
    mutationFn: ({ questionId, data }: { questionId: number; data: EditQuestionRequest }) =>
      reviewService.editQuestion(questionId, data),
    onSuccess: (res) => {
      const statusText = res.data?.status === 'APPROVED' ? 'và tự động phê duyệt' : '';
      toast.success(`Đã cập nhật câu hỏi ${statusText} thành công!`);
      invalidateReviewQueries();
    },
    onError: (err: any) => {
      toast.error(err?.response?.data?.message || err?.message || 'Lỗi khi chỉnh sửa câu hỏi');
    },
  });

  // 16.6 Complete session review
  const completeReviewMutation = useMutation({
    mutationFn: (targetSessionId: number) => reviewService.completeSessionReview(targetSessionId),
    onSuccess: () => {
      toast.success('Đã hoàn tất duyệt phiên nộp! Điểm thưởng đã được cộng cho sinh viên.');
      invalidateReviewQueries();
    },
    onError: (err: any) => {
      toast.error(err?.response?.data?.message || err?.message || 'Lỗi khi hoàn tất duyệt phiên');
    },
  });

  return {
    approveQuestions: approveMutation.mutateAsync,
    isApproving: approveMutation.isPending,

    rejectQuestions: rejectMutation.mutateAsync,
    isRejecting: rejectMutation.isPending,

    editQuestion: editQuestionMutation.mutateAsync,
    isEditing: editQuestionMutation.isPending,

    completeReview: completeReviewMutation.mutateAsync,
    isCompleting: completeReviewMutation.isPending,
  };
};
