import { axiosClient } from '@/api/axiosClient';
import type { GlobalResponse } from '@/types/response.types';

export interface FeedbackRequest {
  message: string;
  pagePath?: string;
}

export function sendFeedback(request: FeedbackRequest) {
  return axiosClient.post<unknown, GlobalResponse<null>>('/feedback', request, { skipToast: true });
}
