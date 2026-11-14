import { useInfiniteQuery, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { reportErrorMessage, reportService } from '../services/reportService';
import type { CreateReportRequestDto, ReportFilters, ResolveReportRequest } from '../types/report.types';
import { toast } from '@/stores/useToastStore';

export function useCreateReport() {
  return useMutation({
    mutationFn: (data: CreateReportRequestDto) =>
      reportService.createReport(data),
    onSuccess: () => {
      toast.success('Đã tiếp nhận báo cáo.');
    },
    onError: (error: unknown) => {
      toast.error(reportErrorMessage(error, 'Không thể gửi báo cáo vi phạm. Vui lòng thử lại sau.'));
    },
  });
}

const reportsKey = ['admin-reports'] as const;

export function useReportQueue(filters: ReportFilters) {
  return useInfiniteQuery({
    queryKey: [...reportsKey, 'queue', filters],
    initialPageParam: undefined as number | undefined,
    queryFn: ({ pageParam, signal }) => reportService.queue(filters, pageParam, signal).then(({ data }) => data),
    getNextPageParam: ({ pagination }) => pagination.hasNext ? pagination.after ?? undefined : undefined,
  });
}

export function useReportDetail(id: number) {
  return useQuery({
    queryKey: [...reportsKey, 'detail', id],
    queryFn: ({ signal }) => reportService.detail(id, signal).then(({ data }) => data),
    retry: false,
  });
}

export function useRelatedReports(id: number, enabled: boolean) {
  return useInfiniteQuery({
    queryKey: [...reportsKey, 'related', id],
    initialPageParam: undefined as number | undefined,
    enabled,
    queryFn: ({ pageParam, signal }) => reportService.related(id, pageParam, signal).then(({ data }) => data),
    getNextPageParam: ({ pagination }) => pagination.hasNext ? pagination.after ?? undefined : undefined,
  });
}

export function useReportDecision(id: number) {
  const client = useQueryClient();
  return useMutation({
    mutationFn: (decision: { resolve: ResolveReportRequest } | { actionId: number; reason: string }) =>
      'resolve' in decision ? reportService.resolve(id, decision.resolve)
        : reportService.reverse(decision.actionId, decision.reason),
    onSuccess: () => {
      toast.success('Đã lưu quyết định.');
      return client.invalidateQueries({ queryKey: reportsKey });
    },
    onError: (error) => {
      if (typeof error === 'object' && error && 'code' in error && error.code === '2322') {
        void client.invalidateQueries({ queryKey: reportsKey });
      }
    },
  });
}
