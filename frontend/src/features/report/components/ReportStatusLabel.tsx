import type { AdminReportSummary } from '../types/report.types';
import { outcomeLabels } from './reportLabels';

export function ReportStatusLabel({ report }: { report: AdminReportSummary }) {
  const pending = report.status === 'PENDING';
  return <span className={pending
    ? 'inline-flex rounded-full bg-amber-100 px-2.5 py-1 text-xs font-semibold text-amber-900 dark:bg-amber-950 dark:text-amber-200'
    : 'inline-flex rounded-full bg-muted px-2.5 py-1 text-xs font-semibold text-muted-foreground'}>
    {pending ? 'Chờ xử lý' : report.outcome ? outcomeLabels[report.outcome] : 'Đã xử lý'}
  </span>;
}
