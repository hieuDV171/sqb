import { useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import {
  AlertCircle,
  BookOpen,
  CheckCircle2,
  Database,
  Info,
  RefreshCcw,
  ShieldCheck,
  Trophy,
} from 'lucide-react';
import { AdminPageHeader } from './AdminPageHeader';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { EmptyState } from '@/components/ui/empty-state';
import { ErrorState } from '@/components/ui/error-state';
import { Label } from '@/components/ui/label';
import { Skeleton } from '@/components/ui/skeleton';
import { AdminLeaderboardRebuildDialog } from '@/features/gamification/components/AdminLeaderboardRebuildDialog';
import {
  useAdminLeaderboardSubjects,
  useRebuildLeaderboard,
} from '@/features/gamification/hooks/useAdminLeaderboard';
import type { LeaderboardPeriod } from '@/features/gamification/types/gamification.types';
import { cn } from '@/lib/utils';

function getErrorMessage(error: unknown): string {
  if (error && typeof error === 'object' && 'message' in error && typeof error.message === 'string') {
    return error.message;
  }
  return 'Không thể tái tạo bảng xếp hạng lúc này.';
}

export function AdminLeaderboardsPage() {
  const [period, setPeriod] = useState<LeaderboardPeriod>('SEMESTER');
  const [selectedSubjectId, setSelectedSubjectId] = useState('');
  const [isConfirmOpen, setIsConfirmOpen] = useState(false);
  const [lastRebuild, setLastRebuild] = useState<{ label: string; completedAt: Date } | null>(null);

  const subjectsQuery = useAdminLeaderboardSubjects();
  const rebuildMutation = useRebuildLeaderboard();
  const subjects = useMemo(() => subjectsQuery.data ?? [], [subjectsQuery.data]);
  const selectedSubject = useMemo(
    () => subjects.find((subject) => String(subject.subjectId) === selectedSubjectId),
    [selectedSubjectId, subjects]
  );

  const targetLabel = period === 'SEMESTER'
    ? 'bảng xếp hạng chung của học kỳ hiện tại'
    : selectedSubject
      ? `môn ${selectedSubject.code} — ${selectedSubject.name}`
      : 'môn học đã chọn';
  const canRebuild = period === 'SEMESTER' || Boolean(selectedSubject);

  const selectPeriod = (nextPeriod: LeaderboardPeriod) => {
    setPeriod(nextPeriod);
    rebuildMutation.reset();
    setLastRebuild(null);
  };

  const openConfirmation = () => {
    if (!canRebuild) return;
    rebuildMutation.reset();
    setIsConfirmOpen(true);
  };

  const confirmRebuild = async () => {
    try {
      await rebuildMutation.mutateAsync({
        type: period,
        subjectId: period === 'SUBJECT' ? Number(selectedSubjectId) : undefined,
      });
      setLastRebuild({ label: targetLabel, completedAt: new Date() });
      setIsConfirmOpen(false);
    } catch {
      // Giữ dialog mở để hiển thị lỗi và cho phép thử lại.
    }
  };

  return (
    <div className="space-y-6">
      <AdminPageHeader icon={Database} title="Đồng bộ bảng xếp hạng"
        description="Tính lại điểm từ PostgreSQL và thay dữ liệu Redis khi bảng xếp hạng bị lệch hoặc sau một đợt bảo trì."
        action={
          <Link to="/leaderboard"
            className="inline-flex min-h-11 items-center gap-2 rounded-xl border border-border px-4 text-sm font-semibold hover:bg-muted focus-visible:outline-2 focus-visible:outline-ring">
            <Trophy className="size-4" aria-hidden="true" /> Xem bảng xếp hạng
          </Link>
        } />

      <div className="grid gap-6 lg:grid-cols-[minmax(0,1fr)_22rem]">
        <Card className="rounded-2xl border border-border bg-card">
          <CardHeader className="border-b border-slate-100 dark:border-slate-800">
            <CardTitle className="flex items-center gap-2 text-lg text-slate-950 dark:text-white">
              <RefreshCcw className="size-5 text-indigo-600 dark:text-indigo-400" aria-hidden="true" />
              Chọn phạm vi tái tạo
            </CardTitle>
            <CardDescription>
              Chỉ bảng xếp hạng trong phạm vi được chọn sẽ bị thay thế trong Redis.
            </CardDescription>
          </CardHeader>
          <CardContent className="space-y-6 pt-1">
            <fieldset>
              <legend className="mb-3 text-sm font-semibold text-slate-900 dark:text-slate-100">Phạm vi dữ liệu</legend>
              <div className="grid gap-3 sm:grid-cols-2">
                {([
                  {
                    value: 'SEMESTER' as const,
                    title: 'Toàn học kỳ',
                    description: 'Tính lại bảng xếp hạng chung của học kỳ đang hoạt động.',
                    icon: Trophy,
                  },
                  {
                    value: 'SUBJECT' as const,
                    title: 'Theo môn học',
                    description: 'Chỉ đồng bộ bảng xếp hạng của một môn cụ thể.',
                    icon: BookOpen,
                  },
                ]).map((option) => {
                  const Icon = option.icon;
                  const checked = period === option.value;
                  return (
                    <label
                      key={option.value}
                      className={cn(
                        'flex min-h-28 cursor-pointer items-start gap-3 rounded-2xl border p-4 transition-all duration-150 active:scale-[0.99] has-[:focus-visible]:ring-2 has-[:focus-visible]:ring-indigo-500 has-[:focus-visible]:ring-offset-2 dark:has-[:focus-visible]:ring-offset-slate-950',
                        checked
                          ? 'border-indigo-500 bg-indigo-50 ring-2 ring-indigo-500/15 dark:border-indigo-500 dark:bg-indigo-950/40'
                          : 'border-slate-200 bg-white hover:border-slate-300 hover:bg-slate-50 dark:border-slate-700 dark:bg-slate-900 dark:hover:border-slate-600 dark:hover:bg-slate-800/60'
                      )}
                    >
                      <input
                        type="radio"
                        name="leaderboard-period"
                        value={option.value}
                        checked={checked}
                        onChange={() => selectPeriod(option.value)}
                        className="sr-only"
                      />
                      <span className={cn(
                        'flex size-10 shrink-0 items-center justify-center rounded-xl',
                        checked
                          ? 'bg-indigo-600 text-white'
                          : 'bg-slate-100 text-slate-500 dark:bg-slate-800 dark:text-slate-300'
                      )}>
                        <Icon className="size-5" aria-hidden="true" />
                      </span>
                      <span className="min-w-0">
                        <span className="flex items-center gap-2 font-semibold text-slate-950 dark:text-white">
                          {option.title}
                          {checked && <CheckCircle2 className="size-4 text-indigo-600 dark:text-indigo-400" aria-hidden="true" />}
                        </span>
                        <span className="mt-1 block text-sm leading-relaxed text-slate-600 dark:text-slate-400">
                          {option.description}
                        </span>
                      </span>
                    </label>
                  );
                })}
              </div>
            </fieldset>

            {period === 'SUBJECT' && (
              <div className="rounded-2xl border border-slate-200 bg-slate-50/70 p-4 dark:border-slate-800 dark:bg-slate-950/40">
                {subjectsQuery.isLoading ? (
                  <div aria-label="Đang tải danh sách môn học" className="space-y-2">
                    <Skeleton className="h-4 w-32" />
                    <Skeleton className="h-11 w-full rounded-xl" />
                    <Skeleton className="h-3 w-2/3" />
                  </div>
                ) : subjectsQuery.isError ? (
                  <ErrorState
                    title="Không tải được danh mục môn học"
                    message="Hãy thử tải lại danh mục trước khi tái tạo bảng xếp hạng theo môn."
                    onRetry={() => void subjectsQuery.refetch()}
                    className="p-6 sm:p-7"
                  />
                ) : subjects.length === 0 ? (
                  <EmptyState
                    icon={BookOpen}
                    title="Chưa có môn học"
                    description="Hãy tạo môn học trong Trung tâm quản trị tài khoản trước khi đồng bộ bảng xếp hạng theo môn."
                    className="p-6 sm:p-7"
                  />
                ) : (
                  <div className="space-y-2">
                    <Label htmlFor="leaderboard-subject" className="text-sm font-semibold text-slate-900 dark:text-slate-100">
                      Môn học cần đồng bộ <span className="text-rose-600 dark:text-rose-400">*</span>
                    </Label>
                    <select
                      id="leaderboard-subject"
                      value={selectedSubjectId}
                      onChange={(event) => {
                        setSelectedSubjectId(event.target.value);
                        rebuildMutation.reset();
                        setLastRebuild(null);
                      }}
                      aria-describedby="leaderboard-subject-help"
                      className="min-h-11 w-full cursor-pointer rounded-xl border border-slate-300 bg-white px-3.5 text-base text-slate-900 outline-none transition focus:border-indigo-500 focus:ring-2 focus:ring-indigo-500/20 dark:border-slate-700 dark:bg-slate-900 dark:text-white sm:text-sm"
                    >
                      <option value="">Chọn một môn học…</option>
                      {subjects.map((subject) => (
                        <option key={subject.subjectId} value={subject.subjectId}>
                          {subject.code} — {subject.name}
                        </option>
                      ))}
                    </select>
                    <p id="leaderboard-subject-help" className="text-xs leading-relaxed text-slate-500 dark:text-slate-400">
                      Danh sách lấy từ danh mục môn học dùng chung của hệ thống.
                    </p>
                  </div>
                )}
              </div>
            )}

            {lastRebuild && (
              <div aria-live="polite" className="flex items-start gap-3 rounded-2xl border border-emerald-200 bg-emerald-50 p-4 text-emerald-900 dark:border-emerald-900/60 dark:bg-emerald-950/40 dark:text-emerald-100">
                <CheckCircle2 className="mt-0.5 size-5 shrink-0" aria-hidden="true" />
                <div>
                  <p className="font-semibold">Đồng bộ hoàn tất</p>
                  <p className="mt-1 text-sm leading-relaxed">
                    Đã tái tạo {lastRebuild.label} lúc{' '}
                    {new Intl.DateTimeFormat('vi-VN', { hour: '2-digit', minute: '2-digit', second: '2-digit' }).format(lastRebuild.completedAt)}.
                  </p>
                </div>
              </div>
            )}

            {rebuildMutation.isError && !isConfirmOpen && (
              <div role="alert" className="flex items-start gap-3 rounded-2xl border border-rose-200 bg-rose-50 p-4 text-rose-900 dark:border-rose-900/60 dark:bg-rose-950/40 dark:text-rose-100">
                <AlertCircle className="mt-0.5 size-5 shrink-0" aria-hidden="true" />
                <div>
                  <p className="font-semibold">Tái tạo chưa thành công</p>
                  <p className="mt-1 text-sm">{getErrorMessage(rebuildMutation.error)} Hãy kiểm tra lựa chọn và thử lại.</p>
                </div>
              </div>
            )}

            <div className="flex flex-col gap-3 border-t border-slate-100 pt-5 dark:border-slate-800 sm:flex-row sm:items-center sm:justify-between">
              <p className="max-w-md text-xs leading-relaxed text-slate-500 dark:text-slate-400">
                Thao tác không thay đổi điểm gốc, lịch sử giao dịch hoặc dữ liệu người dùng trong PostgreSQL.
              </p>
              <Button
                type="button"
                onClick={openConfirmation}
                disabled={!canRebuild || subjectsQuery.isLoading || rebuildMutation.isPending}
                className="min-h-11 cursor-pointer rounded-xl bg-indigo-600 px-5 text-white hover:bg-indigo-700 disabled:cursor-not-allowed"
              >
                <RefreshCcw className="size-4" aria-hidden="true" />
                Tái tạo bảng xếp hạng
              </Button>
            </div>
          </CardContent>
        </Card>

        <aside className="space-y-4">
          <Card className="rounded-2xl border border-border bg-card">
            <CardHeader>
              <CardTitle className="flex items-center gap-2 text-base text-slate-950 dark:text-white">
                <ShieldCheck className="size-5 text-emerald-600 dark:text-emerald-400" aria-hidden="true" />
                Nguồn dữ liệu an toàn
              </CardTitle>
            </CardHeader>
            <CardContent className="space-y-3 text-sm leading-relaxed text-slate-600 dark:text-slate-300">
              <p>PostgreSQL là nguồn sự thật. Redis chỉ lưu bảng xếp hạng đã tổng hợp để đọc nhanh.</p>
              <div className="rounded-xl bg-slate-50 p-3 dark:bg-slate-950/50">
                <span className="block text-xs font-semibold uppercase tracking-wide text-slate-500">Luồng đồng bộ</span>
                <span className="mt-1 block font-medium text-slate-900 dark:text-slate-100">Point history → Tổng hợp → Redis ZSET</span>
              </div>
            </CardContent>
          </Card>

          <Card className="rounded-2xl border border-sky-200 bg-sky-50/70 shadow-sm dark:border-sky-900/60 dark:bg-sky-950/30">
            <CardContent className="flex items-start gap-3 pt-1">
              <Info className="mt-0.5 size-5 shrink-0 text-sky-700 dark:text-sky-300" aria-hidden="true" />
              <div className="text-sm leading-relaxed text-sky-950 dark:text-sky-100">
                <p className="font-semibold">Khi nào nên sử dụng?</p>
                <ul className="mt-2 list-disc space-y-1 pl-4 text-sky-900 dark:text-sky-200">
                  <li>Sau khi sửa lỗi cập nhật điểm realtime.</li>
                  <li>Khi số điểm hiển thị lệch dữ liệu PostgreSQL.</li>
                  <li>Sau bảo trì hoặc khôi phục Redis.</li>
                </ul>
              </div>
            </CardContent>
          </Card>
        </aside>
      </div>

      <AdminLeaderboardRebuildDialog
        open={isConfirmOpen}
        targetLabel={targetLabel}
        isPending={rebuildMutation.isPending}
        errorMessage={rebuildMutation.isError ? getErrorMessage(rebuildMutation.error) : undefined}
        onClose={() => {
          if (!rebuildMutation.isPending) setIsConfirmOpen(false);
        }}
        onConfirm={confirmRebuild}
      />
    </div>
  );
}
