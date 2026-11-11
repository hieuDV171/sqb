import { useState, type FormEvent } from 'react';
import { AlertDialog } from '@base-ui/react/alert-dialog';
import { Dialog } from '@base-ui/react/dialog';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { BookOpen, CalendarDays, CalendarRange, CheckCircle2, Circle, Lock, Loader2, Pencil, Plus, Search } from 'lucide-react';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { DatePicker } from '@/components/ui/date-picker';
import { EmptyState } from '@/components/ui/empty-state';
import { ErrorState } from '@/components/ui/error-state';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Skeleton } from '@/components/ui/skeleton';
import { adminService } from '@/services/adminService';
import { toast } from '@/stores/useToastStore';
import type { SemesterResponse } from '@/types/auth.types';
import { AdminPageHeader } from './AdminPageHeader';

const semesterKey = ['admin', 'semesters'] as const;

type SemesterAction = {
  type: 'activate' | 'finalize' | 'deactivate' | 'delete';
  semester: SemesterResponse;
};

const semesterActions = {
  activate: { label: 'Kích hoạt', description: 'Các lớp học và sinh viên nhập sau đó sẽ được ghi vào học kỳ này.' },
  finalize: { label: 'Chốt sổ', description: 'Hệ thống tổng kết điểm và trao thưởng theo quy tắc học kỳ. Thao tác này không thể hoàn tác.' },
  deactivate: { label: 'Đóng', description: 'Hệ thống sẽ không có học kỳ đang hoạt động cho tới khi bạn kích hoạt kỳ mới.' },
  delete: { label: 'Xóa', description: 'Học kỳ sẽ bị xóa nếu không còn lớp học phần trực thuộc.' },
};

function errorMessage(error: unknown, fallback: string) {
  return error && typeof error === 'object' && 'message' in error && typeof error.message === 'string'
    ? error.message : fallback;
}

function EditSemesterStartDate({ semester, disabled }: { semester: SemesterResponse; disabled: boolean }) {
  const queryClient = useQueryClient();
  const [open, setOpen] = useState(false);
  const [date, setDate] = useState('');
  const fieldId = `semester-${semester.semesterId}-start-date`;
  const update = useMutation({
    mutationFn: (value: string) => adminService.updateSemesterStartDate(semester.semesterId, value),
    onSuccess: async () => {
      toast.success(`Đã cập nhật ngày khai giảng học kỳ ${semester.name}`);
      await queryClient.invalidateQueries({ queryKey: semesterKey });
      setOpen(false);
    },
  });

  return (
    <Dialog.Root open={open} onOpenChange={(next) => {
      if (update.isPending) return;
      if (next) {
        setDate(semester.startDate ?? '');
        update.reset();
      }
      setOpen(next);
    }}>
      <Dialog.Trigger render={<Button type="button" variant="outline" size="sm" disabled={disabled}
        aria-label={`Sửa ngày khai giảng học kỳ ${semester.name}`}
        className="min-h-11 border-academic/30 bg-academic-soft px-2 text-academic hover:bg-academic/15 hover:text-academic sm:px-3" />}>
        <Pencil aria-hidden="true" className="hidden sm:block" /> Sửa ngày
      </Dialog.Trigger>
      <Dialog.Portal>
        <Dialog.Backdrop className="fixed inset-0 z-50 bg-black/60" />
        <Dialog.Popup className="fixed inset-x-4 top-1/2 z-50 mx-auto max-w-sm -translate-y-1/2 space-y-4 rounded-2xl border border-border bg-card p-5 shadow-xl">
          <Dialog.Title className="text-lg font-bold">Sửa ngày khai giảng</Dialog.Title>
          <Dialog.Description className="text-sm text-muted-foreground">Học kỳ {semester.name}</Dialog.Description>
          <form className="space-y-4" onSubmit={(event) => {
            event.preventDefault();
            if (date && date !== semester.startDate && !update.isPending) update.mutate(date);
          }}>
            <div className="space-y-1.5">
              <Label htmlFor={fieldId}>Ngày khai giảng mới</Label>
              <DatePicker id={fieldId} value={date} onChange={setDate} disabled={update.isPending} />
            </div>
            {update.isError && <p role="alert" className="text-sm text-destructive">{errorMessage(update.error, 'Không thể cập nhật ngày khai giảng.')}</p>}
            <div className="flex justify-end gap-2">
              <Dialog.Close render={<Button type="button" variant="outline" disabled={update.isPending} />}>Hủy</Dialog.Close>
              <Button type="submit" disabled={!date || date === semester.startDate || update.isPending}>
                {update.isPending ? 'Đang lưu…' : 'Lưu thay đổi'}
              </Button>
            </div>
          </form>
        </Dialog.Popup>
      </Dialog.Portal>
    </Dialog.Root>
  );
}

function SemesterSettings() {
  const queryClient = useQueryClient();
  const [name, setName] = useState('');
  const [startDate, setStartDate] = useState('');
  const [action, setAction] = useState<SemesterAction | null>(null);
  const semesters = useQuery({
    queryKey: semesterKey,
    queryFn: async () => (await adminService.getAllSemesters()).data,
  });
  const create = useMutation({
    mutationFn: adminService.createSemester,
    onSuccess: async ({ data }) => {
      setName('');
      setStartDate('');
      toast.success(`Đã tạo học kỳ ${data.name}`);
      await queryClient.invalidateQueries({ queryKey: semesterKey });
    },
  });
  const change = useMutation({
    mutationFn: async ({ type, semester }: SemesterAction) => {
      if (type === 'finalize' || type === 'deactivate') {
        const current = (await adminService.getAllSemesters()).data.find((item) => item.active);
        if (current?.semesterId !== semester.semesterId || current.isFinalize !== (type === 'deactivate')) {
          await queryClient.invalidateQueries({ queryKey: semesterKey });
          throw new Error('Học kỳ đang hoạt động hoặc trạng thái chốt sổ đã thay đổi. Hãy kiểm tra lại danh sách.');
        }
      }
      switch (type) {
        case 'activate': await adminService.activateSemester(semester.semesterId); break;
        case 'finalize': await adminService.finalizeSemester(); break;
        case 'deactivate': await adminService.deactivateAllSemesters(); break;
        case 'delete': await adminService.deleteSemester(semester.semesterId); break;
      }
    },
    onSuccess: async () => {
      toast.success('Đã cập nhật học kỳ');
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: semesterKey }),
        queryClient.invalidateQueries({ queryKey: ['gamification'] }),
      ]);
      setAction(null);
    },
  });
  const list = semesters.data ?? [];
  const active = list.find((semester) => semester.active);
  const busy = create.isPending || change.isPending;

  function start(type: SemesterAction['type'], semester: SemesterResponse) {
    change.reset();
    setAction({ type, semester });
  }

  function submit(event: FormEvent) {
    event.preventDefault();
    if (name.trim() && !busy) {
      create.mutate({ name: name.trim(), startDate: startDate || undefined });
    }
  }

  return (
    <section aria-labelledby="admin-semesters" className="space-y-4">
      <div className="rounded-2xl border border-border bg-card p-4 sm:p-5">
        <h2 id="admin-semesters" className="text-lg font-bold">Quản lý học kỳ</h2>
        <p className="mt-1 text-sm text-muted-foreground">Tạo học kỳ, kích hoạt, chốt sổ và đóng kỳ theo thứ tự.</p>
        <form onSubmit={submit} className="mt-4 flex flex-wrap items-end gap-3">
          <div className="w-full space-y-1.5 sm:w-60">
            <Label htmlFor="semester-name">Tên học kỳ</Label>
            <Input id="semester-name" value={name} onChange={(event) => setName(event.target.value)}
              placeholder="VD: 2026.1" required disabled={create.isPending} />
          </div>
          <div className="w-full space-y-1.5 sm:w-52">
            <Label htmlFor="semester-start-date">Ngày khai giảng (tùy chọn)</Label>
            <DatePicker id="semester-start-date" value={startDate} onChange={setStartDate} disabled={create.isPending} allowClear />
          </div>
          <Button type="submit" className="min-h-11" disabled={busy || !name.trim()}>
            {create.isPending ? <Loader2 aria-hidden="true" className="motion-safe:animate-spin" /> : <Plus aria-hidden="true" />}
            Tạo học kỳ
          </Button>
        </form>
        {create.isError && <p role="alert" className="mt-3 text-sm text-destructive">{errorMessage(create.error, 'Không thể tạo học kỳ.')}</p>}
      </div>

      {semesters.isPending ? <Skeleton className="h-40 rounded-2xl" aria-label="Đang tải học kỳ" />
        : semesters.isError ? <ErrorState title="Không thể tải học kỳ" onRetry={() => void semesters.refetch()} />
        : list.length === 0 ? <EmptyState icon={CalendarDays} title="Chưa có học kỳ" />
        : (
          <div className="rounded-2xl border border-border bg-card">
            <div className="border-b border-border px-4 py-3">
              <h3 id="admin-semester-list" className="font-semibold">Danh sách học kỳ</h3>
              <p className="text-sm text-muted-foreground">{active ? `Đang hoạt động · ${active.name}` : 'Chưa có học kỳ đang hoạt động'}</p>
            </div>
            <table aria-labelledby="admin-semester-list" className="w-full table-fixed text-left">
              <thead className="border-b border-border bg-surface-subtle text-xs text-muted-foreground sm:text-sm">
                <tr>
                  <th scope="col" className="px-2 py-2.5 font-medium sm:px-4">Học kỳ</th>
                  <th scope="col" className="px-2 py-2.5 font-medium sm:px-4">Ngày khai giảng</th>
                  <th scope="col" className="px-2 py-2.5 font-medium sm:px-4">Trạng thái</th>
                  <th scope="col" className="w-[30%] px-2 py-2.5 text-right font-medium sm:w-[36%] sm:px-4">Thao tác</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-border">
                {list.map((semester) => (
                  <tr key={semester.semesterId}>
                    <th scope="row" className="px-2 py-3 align-middle font-semibold sm:px-4">
                      <div className="flex flex-wrap items-baseline gap-x-2">
                        <span className="min-w-0 wrap-break-word">{semester.name}</span>
                        <span className="font-mono text-xs font-normal text-muted-foreground">#{semester.semesterId}</span>
                      </div>
                    </th>
                    <td className="px-2 py-3 align-middle text-xs break-words tabular-nums sm:px-4 sm:text-sm">
                      {semester.startDate
                        ? <time dateTime={semester.startDate}>{semester.startDate.split('-').reverse().join('/')}</time>
                        : <span className="text-muted-foreground">Chưa nhập</span>}
                    </td>
                    <td className="px-2 py-3 align-middle sm:px-4">
                      <div className="flex flex-wrap gap-1.5">
                        {semester.active && <Badge className="h-auto border border-success/30 bg-success-soft px-1.5 whitespace-normal text-success hover:bg-success-soft sm:px-2.5"><Circle aria-hidden="true" className="hidden size-2 shrink-0 fill-current sm:block" />Đang hoạt động</Badge>}
                        {semester.isFinalize && <Badge className="h-auto border border-gamification/30 bg-gamification-soft px-1.5 whitespace-normal text-gamification hover:bg-gamification-soft sm:px-2.5"><Lock aria-hidden="true" className="hidden size-3 shrink-0 sm:block" />Đã chốt sổ</Badge>}
                        {!semester.active && !semester.isFinalize && (
                          <Badge className="h-auto border border-warning/30 bg-warning-soft px-1.5 whitespace-normal text-warning hover:bg-warning-soft sm:px-2.5">Chưa kích hoạt</Badge>
                        )}
                      </div>
                    </td>
                    <td className="px-2 py-3 align-middle sm:px-4">
                      <div className="@container ml-auto max-w-72">
                        <div className="grid items-center justify-items-end gap-2 @min-[12rem]:grid-cols-[max-content_minmax(0,1fr)] [&_button]:max-w-full [&_button]:whitespace-normal">
                          <EditSemesterStartDate semester={semester} disabled={busy} />
                          <div className="flex min-w-0 flex-wrap justify-end gap-2">
                            {semester.active ? (
                              <Button type="button" variant="outline" size="sm" disabled={busy}
                                className="min-h-11 border-warning/30 bg-warning-soft px-2 text-warning hover:bg-warning/15 hover:text-warning sm:px-3"
                                onClick={() => start(semester.isFinalize ? 'deactivate' : 'finalize', semester)}>
                                {semester.isFinalize ? <Lock aria-hidden="true" className="hidden sm:block" /> : <CheckCircle2 aria-hidden="true" className="hidden sm:block" />}
                                {semester.isFinalize ? 'Đóng học kỳ' : 'Chốt sổ'}
                              </Button>
                            ) : (
                              <>
                                {!semester.isFinalize && <Button type="button" variant="outline" size="sm"
                                  className="min-h-11 border-success/30 bg-success-soft px-2 text-success hover:bg-success/15 hover:text-success sm:px-3"
                                  disabled={busy || Boolean(active)} onClick={() => start('activate', semester)}>Kích hoạt</Button>}
                                <Button type="button" variant="destructive" size="sm" className="min-h-11 px-2 sm:px-3" disabled={busy}
                                  aria-label={`Xóa học kỳ ${semester.name}`} onClick={() => start('delete', semester)}>Xóa</Button>
                              </>
                            )}
                          </div>
                        </div>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

      <AlertDialog.Root open={action !== null} onOpenChange={(open) => { if (!open && !change.isPending) setAction(null); }}>
        {action && (
          <AlertDialog.Portal>
            <AlertDialog.Backdrop className="fixed inset-0 z-50 bg-black/60" />
            <AlertDialog.Popup className="fixed inset-x-4 top-1/2 z-50 mx-auto max-h-[calc(100dvh-2rem)] max-w-md -translate-y-1/2 space-y-4 overflow-y-auto rounded-2xl border border-border bg-card p-5 text-foreground shadow-xl">
              <AlertDialog.Title className="text-lg font-bold">{semesterActions[action.type].label} học kỳ {action.semester.name}?</AlertDialog.Title>
              <AlertDialog.Description className="text-sm text-muted-foreground">{semesterActions[action.type].description}</AlertDialog.Description>
              {change.isError && <p role="alert" className="text-sm text-destructive">{errorMessage(change.error, 'Không thể cập nhật học kỳ.')}</p>}
              <div className="flex flex-wrap justify-end gap-2">
                <AlertDialog.Close render={<Button type="button" variant="outline" className="min-h-11" disabled={change.isPending} />}>Hủy</AlertDialog.Close>
                <Button type="button" variant={action.type === 'delete' ? 'destructive' : 'default'} className="min-h-11"
                  disabled={change.isPending} onClick={() => change.mutate(action)}>
                  {change.isPending ? 'Đang xử lý…' : semesterActions[action.type].label}
                </Button>
              </div>
            </AlertDialog.Popup>
          </AlertDialog.Portal>
        )}
      </AlertDialog.Root>
    </section>
  );
}

function SubjectSettings() {
  const queryClient = useQueryClient();
  const [code, setCode] = useState('');
  const [name, setName] = useState('');
  const [search, setSearch] = useState('');
  const subjects = useQuery({
    queryKey: ['subjects', 'admin-settings'],
    queryFn: async () => (await adminService.getAllSubjects()).data,
  });
  const create = useMutation({
    mutationFn: adminService.createSubject,
    onSuccess: async ({ data }) => {
      setCode('');
      setName('');
      toast.success(`Đã tạo môn ${data.code}`);
      await queryClient.invalidateQueries({ queryKey: ['subjects'] });
    },
  });
  const keyword = search.trim().toLocaleLowerCase('vi-VN');
  const results = (subjects.data ?? []).filter((subject) =>
    `${subject.code} ${subject.name}`.toLocaleLowerCase('vi-VN').includes(keyword),
  );

  function submit(event: FormEvent) {
    event.preventDefault();
    if (code.trim() && name.trim() && !create.isPending) create.mutate({ code: code.trim(), name: name.trim() });
  }

  return (
    <section aria-labelledby="admin-subjects" className="space-y-4">
      <div className="rounded-2xl border border-border bg-card p-4 sm:p-5">
        <h2 id="admin-subjects" className="text-lg font-bold">Quản lý môn học</h2>
        <p className="mt-1 text-sm text-muted-foreground">Thêm môn vào danh mục dùng chung của trường.</p>
        <form onSubmit={submit} className="mt-4 flex flex-wrap items-end gap-3">
          <div className="w-full space-y-1.5 sm:w-36">
            <Label htmlFor="subject-code">Mã môn</Label>
            <Input id="subject-code" value={code} onChange={(event) => setCode(event.target.value.toUpperCase())}
              placeholder="VD: IT4409" maxLength={10} required disabled={create.isPending} />
          </div>
          <div className="w-full min-w-0 space-y-1.5 sm:min-w-56 sm:flex-1">
            <Label htmlFor="subject-name">Tên môn học</Label>
            <Input id="subject-name" value={name} onChange={(event) => setName(event.target.value)} required disabled={create.isPending} />
          </div>
          <Button type="submit" className="min-h-11" disabled={create.isPending || !code.trim() || !name.trim()}>
            {create.isPending ? <Loader2 aria-hidden="true" className="motion-safe:animate-spin" /> : <Plus aria-hidden="true" />}
            Tạo môn học
          </Button>
        </form>
        {create.isError && <p role="alert" className="mt-3 text-sm text-destructive">{errorMessage(create.error, 'Không thể tạo môn học.')}</p>}
      </div>

      {subjects.isPending ? <Skeleton className="h-40 rounded-2xl" aria-label="Đang tải môn học" />
        : subjects.isError ? <ErrorState title="Không thể tải môn học" onRetry={() => void subjects.refetch()} />
        : (
          <div className="rounded-2xl border border-border bg-card">
            <div className="border-b border-border p-4">
              <Label htmlFor="subject-search" className="mb-2">Tìm trong danh mục môn học</Label>
              <div className="relative max-w-sm">
                <Search aria-hidden="true" className="pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2 text-muted-foreground" />
                <Input id="subject-search" type="search" value={search} onChange={(event) => setSearch(event.target.value)}
                  className="pl-9" placeholder="Nhập mã hoặc tên môn" />
              </div>
            </div>
            {results.length === 0 ? (
              <p role="status" className="p-4 text-sm text-muted-foreground">{keyword ? 'Không tìm thấy môn phù hợp.' : 'Chưa có môn học.'}</p>
            ) : (
              <table aria-label="Danh mục môn học" className="w-full table-fixed text-left text-sm">
                <thead className="border-b border-border bg-surface-subtle text-muted-foreground">
                  <tr>
                    <th scope="col" className="w-28 px-4 py-2.5 font-medium sm:w-40">Mã môn</th>
                    <th scope="col" className="px-4 py-2.5 font-medium">Tên môn học</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-border">
                  {results.map((subject) => (
                    <tr key={subject.subjectId}>
                      <th scope="row" className="px-4 py-3 align-top font-medium">
                        <span className="inline-block max-w-full wrap-anywhere rounded-md bg-academic-soft px-2 py-0.5 font-mono text-academic">{subject.code}</span>
                      </th>
                      <td className="px-4 py-3 align-top font-medium wrap-anywhere">{subject.name}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
        )}
    </section>
  );
}

export function AdminSettingsPage() {
  const [section, setSection] = useState<'semesters' | 'subjects'>('semesters');
  return (
    <div className="mx-auto max-w-6xl space-y-4 pb-8">
      <AdminPageHeader icon={CalendarRange} title="Cấu hình học vụ"
        description="Quản lý học kỳ và danh mục môn học của hệ thống." />
      <div role="group" aria-label="Mục cấu hình học vụ" className="flex flex-wrap gap-2 rounded-2xl border border-border bg-card p-2">
        <Button type="button" variant={section === 'semesters' ? 'secondary' : 'ghost'} aria-pressed={section === 'semesters'}
          className="min-h-11" onClick={() => setSection('semesters')}><CalendarDays aria-hidden="true" /> Học kỳ</Button>
        <Button type="button" variant={section === 'subjects' ? 'secondary' : 'ghost'} aria-pressed={section === 'subjects'}
          className="min-h-11" onClick={() => setSection('subjects')}><BookOpen aria-hidden="true" /> Môn học</Button>
      </div>
      {section === 'semesters' ? <SemesterSettings /> : <SubjectSettings />}
    </div>
  );
}
