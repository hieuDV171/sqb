import { useMutation } from '@tanstack/react-query';
import { sendFeedback } from '../services/feedbackService';

export function useSendFeedback() {
  return useMutation({ mutationFn: sendFeedback });
}
