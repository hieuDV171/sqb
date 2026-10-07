import { axiosClient } from '@/api/axiosClient';
import type { GlobalResponse } from '@/types/response.types';
import type {
  CreateReportRequestDto,
  ReportResponseDto,
} from '../types/report.types';

export const reportService = {
  /**
   * 30.1 Gửi báo cáo vi phạm nội dung hoặc người dùng
   * POST /api/v1/report
   */
  createReport: async (
    data: CreateReportRequestDto
  ): Promise<GlobalResponse<ReportResponseDto>> => {
    return (await axiosClient.post<GlobalResponse<ReportResponseDto>>(
      '/report',
      data
    )) as any;
  },
};
