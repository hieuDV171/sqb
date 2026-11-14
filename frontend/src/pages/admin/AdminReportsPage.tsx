import { ArrowUpRight, ShieldAlert } from 'lucide-react';
import { Link, useSearchParams } from 'react-router-dom';
import { Button } from '@/components/ui/button';
import { EmptyState } from '@/components/ui/empty-state';
import { ErrorState } from '@/components/ui/error-state';
import { FilterSelect } from '@/components/ui/filter-select';
import { InfiniteListFooter } from '@/components/ui/infinite-list-footer';
import { Skeleton } from '@/components/ui/skeleton';
import { useReportQueue } from '@/features/report/hooks/useReport';
import { formatReportDate, targetLabels, violationLabels } from '@/features/report/components/reportLabels';
import { ReportStatusLabel } from '@/features/report/components/ReportStatusLabel';
import type { AdminReportSummary, ReportFilters, ReportTargetType, ViolationType } from '@/features/report/types/report.types';
import { AdminPageHeader } from './AdminPageHeader';

const statusOptions = [
  { value: 'PENDING', label: 'Chờ xử lý' },
  { value: 'RESOLVED', label: 'Đã xử lý' },
  { value: 'ALL', label: 'Tất cả trạng thái' },
];
const targetOptions = [{ value: '', label: 'Tất cả đối tượng' },
  ...Object.entries(targetLabels).map(([value, label]) => ({ value, label }))];
const violationOptions = [{ value: '', label: 'Tất cả lý do' },
  ...Object.entries(violationLabels).map(([value, label]) => ({ value, label }))];

function ReportTarget({ report }: { report: AdminReportSummary }) {
  return (
    <div className="min-w-0">
      <p className="font-semibold">{report.targetType ? targetLabels[report.targetType] ?? 'Đối tượng' : 'Đối tượng'}
        {report.targetId != null && <span className="ml-1.5 font-normal text-muted-foreground">#{report.targetId}</span>}
      </p>
      {report.preview && <p className="mt-0.5 line-clamp-2 break-words text-sm text-muted-foreground">{report.preview}</p>}
    </div>
  );
}

function OpenReport({ id, search }: { id: number; search: string }) {
  return <Button nativeButton={false} variant="ghost" className="min-h-11"
    render={<Link to={{ pathname: `/admin/reports/${id}`, search }} aria-label={`Mở báo cáo #${id}`} />}>
    Xem <ArrowUpRight className="size-4" aria-hidden="true" />
  </Button>;
}

export function AdminReportsPage() {
  const [params, setParams] = useSearchParams();
  const selectedStatus = params.get('status');
  const target = params.get('target');
  const violation = params.get('violation');
  const filters: ReportFilters = {
    status: selectedStatus === 'RESOLVED' || selectedStatus === 'ALL' ? selectedStatus : 'PENDING',
    targetType: target && Object.hasOwn(targetLabels, target) ? target as ReportTargetType : undefined,
    violationType: violation && Object.hasOwn(violationLabels, violation) ? violation as ViolationType : undefined,
  };
  const query = useReportQueue(filters);
  const items = query.data?.pages.flatMap((page) => page.items) ?? [];
  const search = params.toString();

  function filter(key: string, value: string) {
    const next = new URLSearchParams(params);
    if (value) next.set(key, value);
    else next.delete(key);
    setParams(next, { replace: true });
  }

  return (
    <div className="mx-auto max-w-6xl space-y-4 pb-8">
      <AdminPageHeader icon={ShieldAlert} title="Báo cáo vi phạm"
        description="Xem bằng chứng và xử lý báo cáo về bài viết, bình luận và tài khoản." />
      <section aria-label="Lọc báo cáo" className="grid gap-3 rounded-2xl border border-border bg-card p-4 sm:grid-cols-3">
        <FilterSelect label="Trạng thái" value={filters.status} options={statusOptions} onChange={(value) => filter('status', value)} />
        <FilterSelect label="Đối tượng" value={filters.targetType ?? ''} options={targetOptions} onChange={(value) => filter('target', value)} />
        <FilterSelect label="Lý do vi phạm" value={filters.violationType ?? ''} options={violationOptions} onChange={(value) => filter('violation', value)} />
      </section>
      {query.isPending ? (
        <div className="space-y-2" aria-busy="true" aria-label="Đang tải báo cáo">
          {[0, 1, 2].map((row) => <Skeleton key={row} className="h-20 rounded-2xl" />)}
        </div>
      ) : query.isLoadingError ? (
        <ErrorState title="Không tải được báo cáo" onRetry={() => void query.refetch()} />
      ) : items.length === 0 ? (
        <EmptyState icon={ShieldAlert} title="Không có báo cáo phù hợp"
          description="Thay đổi bộ lọc để xem các báo cáo khác." className="border-border bg-card" />
      ) : (
        <>
          <div className="hidden overflow-hidden rounded-2xl border border-border bg-card lg:block">
            <table className="w-full table-fixed text-left text-sm">
              <thead className="border-b border-border bg-muted/50 text-muted-foreground">
                <tr><th className="w-[34%] px-4 py-3" scope="col">Đối tượng</th>
                  <th className="w-[23%] px-4 py-3" scope="col">Lý do</th>
                  <th className="w-[17%] px-4 py-3" scope="col">Ngày gửi</th>
                  <th className="w-[17%] px-4 py-3" scope="col">Trạng thái</th>
                  <th className="px-3 py-3 text-right" scope="col"><span className="sr-only">Mở báo cáo</span></th></tr>
              </thead>
              <tbody className="divide-y divide-border">
                {items.map((report) => <tr key={report.id}>
                  <td className="px-4 py-3"><ReportTarget report={report} /></td>
                  <td className="px-4 py-3">{report.violationType ? violationLabels[report.violationType] : '—'}</td>
                  <td className="px-4 py-3 tabular-nums text-muted-foreground">{formatReportDate(report.createdAt)}</td>
                  <td className="px-4 py-3"><ReportStatusLabel report={report} />
                    {report.pendingCount > 1 && <p className="mt-1 text-xs text-muted-foreground">{report.pendingCount} đơn chờ cùng đối tượng</p>}
                  </td>
                  <td className="px-3 py-3 text-right"><OpenReport id={report.id} search={search} /></td>
                </tr>)}
              </tbody>
            </table>
          </div>
          <ul className="space-y-2 lg:hidden">
            {items.map((report) => <li key={report.id} className="space-y-3 rounded-2xl border border-border bg-card p-4">
              <div className="flex flex-wrap items-center justify-between gap-2">
                <ReportStatusLabel report={report} />
                <span className="text-xs text-muted-foreground">{formatReportDate(report.createdAt)}</span>
              </div>
              <ReportTarget report={report} />
              <div className="flex items-center justify-between gap-2">
                <div className="min-w-0">
                  <p className="text-sm">{report.violationType ? violationLabels[report.violationType] : '—'}</p>
                  {report.pendingCount > 1 && <p className="text-xs text-muted-foreground">{report.pendingCount} đơn chờ cùng đối tượng</p>}
                </div>
                <OpenReport id={report.id} search={search} />
              </div>
            </li>)}
          </ul>
          {query.isRefetchError && <p role="status" className="text-sm text-destructive">
            Chưa làm mới được danh sách. <button type="button" className="min-h-11 underline" onClick={() => void query.refetch()}>Thử lại</button>
          </p>}
          <InfiniteListFooter query={query} auto={false} />
        </>
      )}
    </div>
  );
}
