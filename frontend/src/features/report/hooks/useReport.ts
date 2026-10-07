import { useMutation } from '@tanstack/react-query';
import { reportService } from '../services/reportService';
import type { CreateReportRequestDto } from '../types/report.types';
import { toast } from '@/stores/useToastStore';

export function useCreateReport() {
  return useMutation({
    mutationFn: (data: CreateReportRequestDto) =>
      reportService.createReport(data),
    onSuccess: () => {
      toast.success('Gửi báo cáo thành công. Ban quản trị sẽ sớm xem xét xử lý!');
    },
    onError: (err: any) => {
      const msg =
        err?.response?.data?.message ||
        'Không thể gửi báo cáo vi phạm. Vui lòng thử lại sau.';
      toast.error(msg);
    },
  });
}
