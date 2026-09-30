import { useMutation, useQueryClient } from '@tanstack/react-query';
import { sessionService } from '../services/sessionService';
import { toast } from '@/stores/useToastStore';
import type {
  ProposeSessionRequest,
  UpdateSubmissionSessionRequest,
  Game2PredictionRequest,
} from '../types/session.types';

export function useSessionMutations() {
  const queryClient = useQueryClient();

  const proposeMutation = useMutation({
    mutationFn: async (data: ProposeSessionRequest) => {
      const res = await sessionService.proposeSession(data);
      return res.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['my-submissions'] });
      toast.success('Đề xuất phiên câu hỏi thành công!');
    },
    onError: (err: any) => {
      toast.error(err?.response?.data?.message || err?.message || 'Lỗi khi đề xuất phiên');
    },
  });

  const updateMutation = useMutation({
    mutationFn: async ({
      sessionId,
      data,
    }: {
      sessionId: number;
      data: UpdateSubmissionSessionRequest;
    }) => {
      const res = await sessionService.updateSubmission(sessionId, data);
      return res.data;
    },
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ['my-submissions'] });
      queryClient.invalidateQueries({
        queryKey: ['my-submission-detail', variables.sessionId],
      });
      toast.success('Cập nhật phiên nộp thành công!');
    },
    onError: (err: any) => {
      toast.error(err?.response?.data?.message || err?.message || 'Lỗi khi cập nhật phiên nộp');
    },
  });

  const deleteMutation = useMutation({
    mutationFn: async (sessionId: number) => {
      const res = await sessionService.deleteSubmission(sessionId);
      return res.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['my-submissions'] });
      toast.success('Đã xóa phiên đề xuất nháp thành công!');
    },
    onError: (err: any) => {
      toast.error(err?.response?.data?.message || err?.message || 'Lỗi khi xóa phiên');
    },
  });

  const predictGame2Mutation = useMutation({
    mutationFn: async (data: Game2PredictionRequest) => {
      const res = await sessionService.predictGame2(data);
      return res.data;
    },
    onSuccess: () => {
      toast.success('Đã ghi nhận dự đoán Gamification Game 2!');
    },
    onError: (err: any) => {
      toast.warning(err?.response?.data?.message || 'Không thể gửi dự đoán Game 2');
    },
  });

  return {
    proposeMutation,
    updateMutation,
    deleteMutation,
    predictGame2Mutation,
  };
}
