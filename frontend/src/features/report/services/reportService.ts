import { axiosClient } from '@/api/axiosClient';
import type { CursorResponse, GlobalResponse } from '@/types/response.types';
import type {
  AdminReportDetail, AdminReportSummary, CreateReportRequestDto, ReportFilters,
  ReportResponseDto, ResolveReportRequest,
} from '../types/report.types';

const quiet = { skipToast: true };

export const reportService = {
  createReport(data: CreateReportRequestDto) {
    return axiosClient.post<unknown, GlobalResponse<ReportResponseDto>>('/report', data, quiet);
  },
  uploadEvidence(file: File) {
    const data = new FormData();
    data.append('file', file);
    return axiosClient.post<unknown, GlobalResponse<{ key: string }>>('/report/evidence', data, {
      ...quiet, headers: { 'Content-Type': 'multipart/form-data' },
    });
  },
  queue(filters: ReportFilters, after?: number, signal?: AbortSignal) {
    return axiosClient.get<unknown, GlobalResponse<CursorResponse<AdminReportSummary>>>('/admin/reports', {
      ...quiet, params: { ...filters, after }, signal,
    });
  },
  detail(id: number, signal?: AbortSignal) {
    return axiosClient.get<unknown, GlobalResponse<AdminReportDetail>>(`/admin/reports/${id}`, { ...quiet, signal });
  },
  related(id: number, after?: number, signal?: AbortSignal) {
    return axiosClient.get<unknown, GlobalResponse<CursorResponse<AdminReportSummary>>>(`/admin/reports/${id}/related`, {
      ...quiet, params: { after }, signal,
    });
  },
  evidence(id: number) {
    return axiosClient.get<unknown, GlobalResponse<{ url: string }>>(`/admin/reports/${id}/evidence`, quiet);
  },
  importEvidence(id: number) {
    return axiosClient.post<unknown, GlobalResponse<{ url: string }>>(`/admin/reports/${id}/evidence/import`, undefined, quiet);
  },
  resolve(id: number, request: ResolveReportRequest) {
    return axiosClient.post<unknown, GlobalResponse<AdminReportDetail>>(`/admin/reports/${id}/resolve`, request, quiet);
  },
  reverse(id: number, reason: string) {
    return axiosClient.post<unknown, GlobalResponse<AdminReportDetail>>(`/admin/moderation-actions/${id}/reverse`, { reason }, quiet);
  },
};

export function reportErrorMessage(error: unknown, fallback: string) {
  return typeof error === 'object' && error !== null && 'message' in error && typeof error.message === 'string'
    ? error.message : fallback;
}
