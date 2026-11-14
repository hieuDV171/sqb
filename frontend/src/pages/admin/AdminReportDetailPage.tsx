import { useId, useState, type FormEvent, type ReactNode } from 'react';
import { Dialog } from '@base-ui/react/dialog';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { ArrowLeft, ChevronDown, Image, Minus, Plus, ShieldAlert, X, ZoomIn } from 'lucide-react';
import { Link, useLocation, useParams } from 'react-router-dom';
import { Button } from '@/components/ui/button';
import { ErrorState } from '@/components/ui/error-state';
import { FilterSelect } from '@/components/ui/filter-select';
import { InfiniteListFooter } from '@/components/ui/infinite-list-footer';
import { Skeleton } from '@/components/ui/skeleton';
import { Textarea } from '@/components/ui/textarea';
import { useRelatedReports, useReportDecision, useReportDetail } from '@/features/report/hooks/useReport';
import { reportErrorMessage, reportService } from '@/features/report/services/reportService';
import { decisionLabels, formatReportDate, targetLabels, targetStateLabels, violationLabels } from '@/features/report/components/reportLabels';
import type { AdminReportDetail, ReportDecision, ReportPerson, ReportSnapshot, SuspensionDays } from '@/features/report/types/report.types';
import { AdminPageHeader } from './AdminPageHeader';
import { ReportStatusLabel } from '@/features/report/components/ReportStatusLabel';

const durationOptions = [
  { value: '1', label: '24 giờ' }, { value: '7', label: '7 ngày' }, { value: '30', label: '30 ngày' },
];
const evidenceZoomLevels = [1, 1.5, 2, 3, 4, 6, 8];
const evidenceAlt = 'Ảnh bằng chứng do người dùng cung cấp';
const decisionEffects: Record<ReportDecision | 'REVERSE', string> = {
  DISMISS: 'Lưu kết quả xem xét và gửi xác nhận cho người báo cáo.',
  HIDE_CONTENT: 'Gỡ nội dung khỏi các màn liên quan. Ban quản trị có thể khôi phục khi cần.',
  WARN_ACCOUNT: 'Gửi cảnh cáo và lý do đến tài khoản bị báo cáo.',
  SUSPEND_ACCOUNT: 'Thu hồi phiên đăng nhập và ngắt kết nối của tài khoản trong thời hạn đã chọn.',
  REVERSE: 'Gỡ biện pháp đang áp dụng và lưu lý do vào lịch sử.',
};

function Section({ title, children }: { title: string; children: ReactNode }) {
  return <section className="min-w-0 space-y-3 rounded-2xl border border-border bg-card p-4 sm:p-5">
    <h2 className="text-base font-semibold">{title}</h2>
    {children}
  </section>;
}

function PersonName({ person }: { person: ReportPerson | null }) {
  return person ? <>{person.fullName || 'Tài khoản'} <span className="text-muted-foreground">#{person.id}</span></> : <>—</>;
}

function Snapshot({ value, empty }: { value: ReportSnapshot | null; empty: string }) {
  return value ? <>
    <p className="whitespace-pre-wrap wrap-anywhere rounded-xl bg-muted/60 p-3 text-sm leading-relaxed">
      {value.content || 'Không có phần văn bản.'}
    </p>
    <div className="flex flex-wrap gap-x-4 gap-y-1 text-xs text-muted-foreground">
      {value.authorId != null && <span>Tài khoản #{value.authorId}</span>}
      {value.updatedAt && <span>Cập nhật {formatReportDate(value.updatedAt)}</span>}
    </div>
  </> : <p className="text-sm text-muted-foreground">{empty}</p>;
}

function Evidence({ report }: { report: AdminReportDetail }) {
  const client = useQueryClient();
  const [imageFailed, setImageFailed] = useState(false);
  const [previewFailed, setPreviewFailed] = useState(false);
  const [zoomIndex, setZoomIndex] = useState(0);
  const [aspectRatio, setAspectRatio] = useState(1);
  const zoom = evidenceZoomLevels[zoomIndex];
  const image = useMutation({
    mutationFn: () => report.hasEvidence ? reportService.evidence(report.report.id) : reportService.importEvidence(report.report.id),
    onSuccess: () => {
      setImageFailed(false);
      setPreviewFailed(false);
      if (!report.hasEvidence) void client.invalidateQueries({ queryKey: ['admin-reports', 'detail', report.report.id] });
    },
  });
  const url = image.data?.data.url;
  return <Section title="Ảnh bằng chứng">
    {report.hasEvidence || report.hasLegacyEvidence ? <>
      {url && !imageFailed && <Dialog.Root onOpenChange={(open) => {
        if (open) {
          setZoomIndex(0);
          setPreviewFailed(false);
        }
      }}>
        <Dialog.Trigger aria-label="Phóng to ảnh bằng chứng"
          className="group relative block w-full cursor-zoom-in overflow-hidden rounded-xl focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-ring">
          <img className="max-h-96 w-full bg-muted object-contain" src={url} alt={evidenceAlt} referrerPolicy="no-referrer"
            onLoad={({ currentTarget: img }) => setAspectRatio(img.naturalWidth / img.naturalHeight)}
            onError={() => setImageFailed(true)} />
          <span className="absolute right-2 bottom-2 grid size-9 place-items-center rounded-lg bg-card text-foreground shadow group-hover:text-primary">
            <ZoomIn className="size-5" aria-hidden="true" />
          </span>
        </Dialog.Trigger>
        <Dialog.Portal>
          <Dialog.Backdrop className="fixed inset-0 z-50 bg-black/80" />
          <Dialog.Popup className="fixed inset-x-2 top-[3dvh] z-50 mx-auto flex h-[94dvh] max-w-[100rem] flex-col overflow-hidden rounded-2xl border border-border bg-card shadow-xl sm:inset-x-6">
            <header className="flex flex-wrap items-center justify-between gap-x-4 gap-y-1 border-b border-border px-3 py-2 sm:px-4">
              <Dialog.Title className="text-base font-semibold">Ảnh bằng chứng</Dialog.Title>
              <div className="flex items-center gap-1">
                <Button type="button" variant="ghost" size="icon" className="size-11" aria-label="Thu nhỏ ảnh"
                  disabled={previewFailed || zoomIndex === 0} onClick={() => setZoomIndex((index) => index - 1)}>
                  <Minus aria-hidden="true" />
                </Button>
                <Button type="button" variant="ghost" className="min-h-11 min-w-16 px-2 tabular-nums"
                  aria-label={`Mức thu phóng ${zoom * 100}%. Về vừa khung`}
                  disabled={previewFailed || zoomIndex === 0} onClick={() => setZoomIndex(0)}>
                  {zoom * 100}%
                </Button>
                <Button type="button" variant="ghost" size="icon" className="size-11" aria-label="Phóng to ảnh"
                  disabled={previewFailed || zoomIndex === evidenceZoomLevels.length - 1}
                  onClick={() => setZoomIndex((index) => index + 1)}>
                  <Plus aria-hidden="true" />
                </Button>
                <Dialog.Close render={<Button variant="ghost" size="icon" className="size-11" aria-label="Đóng ảnh" />}>
                  <X aria-hidden="true" />
                </Dialog.Close>
              </div>
            </header>
            {previewFailed ? (
              <div className="m-auto space-y-3 p-4 text-center">
                <p role="alert" className="text-sm text-muted-foreground">
                  {reportErrorMessage(image.error, 'Ảnh không tải được hoặc liên kết đã hết hạn.')}
                </p>
                <Button type="button" variant="outline" disabled={image.isPending} onClick={() => image.mutate()}>
                  {image.isPending ? 'Đang tải ảnh…' : 'Tải lại ảnh'}
                </Button>
              </div>
            ) : (
              <div className="min-h-0 flex-1 overflow-auto bg-muted [container-type:size]" tabIndex={0}
                role="region" aria-label="Vùng xem ảnh, có thể cuộn khi phóng to">
                <div className="flex min-h-full min-w-full w-max items-center justify-center">
                  <img src={url} alt={evidenceAlt} referrerPolicy="no-referrer" className="block h-auto max-w-none"
                    style={{ width: `min(${zoom * 100}cqw, ${zoom * 100 * aspectRatio}cqh)` }}
                    onError={() => setPreviewFailed(true)} />
                </div>
              </div>
            )}
          </Dialog.Popup>
        </Dialog.Portal>
      </Dialog.Root>}
      <Button type="button" variant="outline" className="min-h-11" disabled={image.isPending} onClick={() => image.mutate()}>
        <Image aria-hidden="true" /> {image.isPending ? 'Đang tải ảnh…' : image.data ? 'Tải lại ảnh'
          : report.hasEvidence ? 'Xem ảnh bằng chứng' : 'Chuyển và xem ảnh cũ'}
      </Button>
      {(image.isError || imageFailed) && <p role="alert" className="text-sm text-destructive">
        {reportErrorMessage(image.error, 'Ảnh không tải được hoặc liên kết đã hết hạn.')}
      </p>}
    </> : <p className="text-sm text-muted-foreground">Không có ảnh bằng chứng.</p>}
  </Section>;
}

function RelatedReports({ id, search }: { id: number; search: string }) {
  const [open, setOpen] = useState(false);
  const related = useRelatedReports(id, open);
  const items = related.data?.pages.flatMap((page) => page.items) ?? [];
  return <details className="rounded-2xl border border-border bg-card p-4 sm:p-5" onToggle={(event) => setOpen(event.currentTarget.open)}>
    <summary className="flex min-h-11 cursor-pointer list-none items-center justify-between gap-2 rounded-lg font-semibold focus-visible:outline-2 focus-visible:outline-ring">
      Báo cáo cùng đối tượng <ChevronDown className="size-4 text-muted-foreground" aria-hidden="true" />
    </summary>
    {open && <div className="mt-2 space-y-3">
      {related.isPending ? <Skeleton className="h-12 rounded-lg" /> : related.isLoadingError
        ? <ErrorState title="Không tải được báo cáo liên quan" onRetry={() => void related.refetch()} />
        : items.length === 0 ? <p className="text-sm text-muted-foreground">Chưa có đơn khác về đối tượng này.</p>
        : <ul className="divide-y divide-border">
          {items.map((item) => <li key={item.id} className="flex flex-wrap items-center justify-between gap-x-4 py-1">
            <Button nativeButton={false} variant="ghost" className="min-h-11 max-w-full justify-start whitespace-normal px-0 text-left"
              render={<Link to={{ pathname: `/admin/reports/${item.id}`, search }} />}>
              #{item.id} · {item.violationType ? violationLabels[item.violationType] : 'Báo cáo vi phạm'}
            </Button>
            <ReportStatusLabel report={item} />
          </li>)}
        </ul>}
      <InfiniteListFooter query={related} auto={false} />
    </div>}
  </details>;
}

function ReportDecisionDialog({ report, onClose, mode }: {
  report: AdminReportDetail; onClose: () => void; mode: 'review' | 'reverse';
}) {
  const mutation = useReportDecision(report.report.id);
  const [draft, setDraft] = useState<ReportDecision | null>(null);
  const [reason, setReason] = useState('');
  const [days, setDays] = useState<SuspensionDays>(1);
  const reasonId = useId();
  const selected = draft ?? report.allowedActions[0];
  const reversing = mode === 'reverse';
  const currentActionId = report.activeActionId;
  const [reverseActionId] = useState(currentActionId);
  const [openedVersion] = useState(report.version);
  const stillAllowed = reversing ? Boolean(reverseActionId && reverseActionId === currentActionId)
    : selected !== undefined && report.allowedActions.includes(selected) && report.version === openedVersion;
  const reverseLabel = report.report.targetType === 'USER' ? decisionLabels.LIFT_SUSPENSION : decisionLabels.RESTORE_CONTENT;

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!reason.trim() || !stillAllowed || mutation.isPending) return;
    try {
      if (reversing && reverseActionId != null) {
        await mutation.mutateAsync({ actionId: reverseActionId, reason: reason.trim() });
      } else if (selected) {
        await mutation.mutateAsync({ resolve: { action: selected, version: openedVersion, reason: reason.trim(),
          suspensionDays: selected === 'SUSPEND_ACCOUNT' ? days : undefined } });
      }
      onClose();
    } catch {
      // Keep the decision and reason visible while the administrator reviews the error.
    }
  }

  return <Dialog.Root open onOpenChange={(open) => { if (!open && !mutation.isPending) onClose(); }}>
    <Dialog.Portal>
      <Dialog.Backdrop className="fixed inset-0 z-50 bg-black/55" />
      <Dialog.Popup className="fixed inset-x-4 top-1/2 z-50 mx-auto max-h-[calc(100dvh-2rem)] max-w-md -translate-y-1/2 overflow-y-auto rounded-2xl border border-border bg-card p-5 shadow-xl">
        <Dialog.Title className="text-lg font-semibold">{reversing ? reverseLabel : `Xử lý báo cáo #${report.report.id}`}</Dialog.Title>
        <Dialog.Description className="mt-1 text-sm text-muted-foreground">
          {reversing ? decisionEffects.REVERSE : selected ? decisionEffects[selected] : 'Báo cáo đã được xử lý.'}
        </Dialog.Description>
        <form className="mt-4 space-y-4" onSubmit={submit}>
          {!reversing && <FilterSelect label="Quyết định" value={selected ?? ''}
            options={report.allowedActions.map((value) => ({ value, label: decisionLabels[value] }))}
            disabled={mutation.isPending} onChange={(value) => setDraft(value as ReportDecision)} />}
          {!reversing && selected === 'SUSPEND_ACCOUNT' && <FilterSelect label="Thời hạn" value={String(days)}
            options={durationOptions} disabled={mutation.isPending} onChange={(value) => setDays(Number(value) as SuspensionDays)} />}
          <div className="space-y-1.5">
            <label className="text-sm font-medium" htmlFor={reasonId}>Lý do xử lý</label>
            <Textarea id={reasonId} rows={4} maxLength={500} required value={reason} disabled={mutation.isPending}
              onChange={(event) => setReason(event.target.value)} />
          </div>
          {(!stillAllowed || mutation.isError) && <p role="alert" className="text-sm text-destructive">
            {reportErrorMessage(mutation.error, 'Trạng thái đã thay đổi. Đóng hộp thoại để xem quyết định mới nhất.')}
          </p>}
          <div className="flex flex-wrap justify-end gap-2">
            <Button type="button" variant="outline" className="min-h-11" onClick={onClose} disabled={mutation.isPending}>Hủy</Button>
            <Button type="submit" className="min-h-11"
              variant={reversing || selected === 'DISMISS' ? 'default' : 'destructive'}
              disabled={!reason.trim() || !stillAllowed || mutation.isPending}>
              {mutation.isPending ? 'Đang lưu…' : 'Xác nhận'}
            </Button>
          </div>
        </form>
      </Dialog.Popup>
    </Dialog.Portal>
  </Dialog.Root>;
}

function ReportContent({ data, search }: { data: AdminReportDetail; search: string }) {
  const [mode, setMode] = useState<'review' | 'reverse' | null>(null);
  const report = data.report;
  const target = report.targetType ? targetLabels[report.targetType] ?? 'Đối tượng' : 'Đối tượng';
  return <>
    <div className="grid items-start gap-4 lg:grid-cols-[minmax(0,1.45fr)_minmax(0,1fr)]">
      <div className="space-y-4">
        <Section title="Nội dung báo cáo">
          <p className="font-medium">{report.violationType ? violationLabels[report.violationType] : 'Lý do vi phạm'}</p>
          <p className="whitespace-pre-wrap wrap-anywhere text-sm text-muted-foreground">
            {data.description || 'Người gửi không bổ sung mô tả.'}
          </p>
          <p className="text-xs text-muted-foreground">Gửi bởi <PersonName person={data.reporter} /> · {formatReportDate(report.createdAt)}</p>
        </Section>
        <Section title="Bản chụp khi gửi">
          <Snapshot value={data.snapshot} empty="Không còn bản chụp cho báo cáo này." />
        </Section>
        <Section title="Đối tượng hiện tại">
          <div className="flex flex-wrap items-center gap-2 text-sm">
            <span className="font-medium">{target} #{report.targetId ?? '—'}</span>
            <span className="rounded-full bg-muted px-2.5 py-1 text-xs font-medium">{targetStateLabels[report.targetState]}</span>
          </div>
          <Snapshot value={data.current} empty="Đối tượng không còn khả dụng." />
          {data.suspendedUntil && <p className="text-sm text-muted-foreground">Đình chỉ đến {formatReportDate(data.suspendedUntil)}</p>}
          {report.targetState === 'AVAILABLE' && report.targetId != null && (report.targetType === 'POST' || report.targetType === 'USER') && (
            <Button nativeButton={false} variant="outline" className="min-h-11"
              render={<Link to={`/${report.targetType === 'POST' ? 'posts' : 'users'}/${report.targetId}`} />}>Mở {target.toLowerCase()}</Button>
          )}
        </Section>
        <Evidence report={data} />
        <RelatedReports id={report.id} search={search} />
      </div>
      <div className="space-y-4">
        <Section title="Quyết định">
          <ReportStatusLabel report={report} />
          {data.reviewedAt && <p className="text-xs text-muted-foreground">
            <PersonName person={data.reviewer} /> · {formatReportDate(data.reviewedAt)}
          </p>}
          {data.resolutionNote && <p className="whitespace-pre-wrap wrap-anywhere text-sm">{data.resolutionNote}</p>}
          <div className="flex flex-wrap gap-2">
            {data.allowedActions.length > 0 && <Button type="button" className="min-h-11" onClick={() => setMode('review')}>Xử lý báo cáo</Button>}
            {data.activeActionId != null && <Button type="button" variant="outline" className="min-h-11" onClick={() => setMode('reverse')}>
              {report.targetType === 'USER' ? decisionLabels.LIFT_SUSPENSION : decisionLabels.RESTORE_CONTENT}
            </Button>}
          </div>
        </Section>
        <Section title="Lịch sử biện pháp">
          {data.history.length === 0 ? <p className="text-sm text-muted-foreground">Chưa áp dụng biện pháp nào.</p>
            : <ol className="divide-y divide-border">
              {data.history.map((action) => <li key={action.id} className="space-y-1 py-3 first:pt-0 last:pb-0">
                <p className="text-sm font-semibold">{decisionLabels[action.type]}</p>
                <p className="text-xs text-muted-foreground"><PersonName person={action.admin} /> · {formatReportDate(action.createdAt)}</p>
                <p className="whitespace-pre-wrap wrap-anywhere text-sm">{action.reason}</p>
                {action.suspendedUntil && <p className="text-xs text-muted-foreground">Đến {formatReportDate(action.suspendedUntil)}</p>}
                {action.reportId !== report.id && <Link className="inline-flex min-h-11 items-center text-sm underline"
                  to={{ pathname: `/admin/reports/${action.reportId}`, search }}>Báo cáo #{action.reportId}</Link>}
              </li>)}
            </ol>}
        </Section>
      </div>
    </div>
    {mode && <ReportDecisionDialog report={data} mode={mode} onClose={() => setMode(null)} />}
  </>;
}

function ReportDetail({ id }: { id: number }) {
  const { search } = useLocation();
  const query = useReportDetail(id);
  return <div className="mx-auto max-w-6xl space-y-4 pb-8">
    <Button nativeButton={false} variant="ghost" className="min-h-11"
      render={<Link to={{ pathname: '/admin/reports', search }} />}>
      <ArrowLeft aria-hidden="true" /> Danh sách báo cáo
    </Button>
    <AdminPageHeader icon={ShieldAlert} title={`Báo cáo #${id}`} description="Xem căn cứ và lịch sử xử lý của đối tượng." />
    {query.isPending ? <Skeleton className="h-64 rounded-2xl" aria-label="Đang tải chi tiết" />
      : query.isLoadingError ? <ErrorState title="Không tải được báo cáo"
        message={reportErrorMessage(query.error, 'Hãy thử tải lại chi tiết.')} onRetry={() => void query.refetch()} />
        : query.data && <ReportContent key={id} data={query.data} search={search} />}
  </div>;
}

export function AdminReportDetailPage() {
  const id = Number(useParams().reportId);
  return Number.isSafeInteger(id) && id > 0
    ? <ReportDetail id={id} />
    : <ErrorState title="Mã báo cáo không hợp lệ" message="Mở một đơn từ danh sách báo cáo vi phạm." />;
}
