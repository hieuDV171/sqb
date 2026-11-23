import { useState } from 'react';
import { ArrowRight, ClipboardCheck, FileText, Layers, Users } from 'lucide-react';
import { Link } from 'react-router-dom';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { EmptyState } from '@/components/ui/empty-state';
import { ErrorState } from '@/components/ui/error-state';
import { Skeleton } from '@/components/ui/skeleton';
import { LecturerQuestionsExportCard, useLecturerClasses } from '@/features/exam';
import { TopicCatalogDialog } from '@/features/session/components/TopicCatalogDialog';

interface AssignedSubject {
  id: number;
  code: string;
  name: string;
}

export function LecturerQuestionsPage() {
  const classes = useLecturerClasses({ includeHistory: true });
  const [managingSubjectId, setManagingSubjectId] = useState<number>();
  const subjects = new Map<number, AssignedSubject>();
  for (const { subjectId, subjectCode, subjectName } of classes.data ?? []) {
    subjects.set(subjectId, { id: subjectId, code: subjectCode, name: subjectName });
  }
  const managingSubject = managingSubjectId === undefined ? undefined : subjects.get(managingSubjectId);

  return (
    <div className="mx-auto max-w-6xl space-y-6 pb-8">
      <header className="flex flex-wrap items-end justify-between gap-4 border-b border-border pb-5">
        <div className="space-y-2">
          <p className="flex items-center gap-2 text-sm font-semibold text-academic">
            <Layers aria-hidden="true" className="size-4" /> Không gian giảng viên
          </p>
          <h1 className="text-2xl font-bold tracking-tight sm:text-3xl">Ngân hàng câu hỏi môn học</h1>
          <p className="text-sm text-muted-foreground">Quản lý chủ đề, xuất tài liệu ôn tập và xem các lớp học phần phụ trách.</p>
        </div>
        <div className="flex flex-wrap gap-2">
          <Button nativeButton={false} variant="outline" render={<Link to="/lecturer/pending-reviews" />}>
            <ClipboardCheck aria-hidden="true" /> Duyệt câu hỏi
          </Button>
          <Button nativeButton={false} render={<Link to="/lecturer/exams" />}>
            <FileText aria-hidden="true" /> Quản lý đề thi
          </Button>
        </div>
      </header>

      <section aria-labelledby="lecturer-topics-title" className="overflow-hidden rounded-2xl border border-border bg-card text-card-foreground shadow-[var(--card-shadow)]">
        <h2 id="lecturer-topics-title" className="border-b border-border px-4 py-3 text-lg font-semibold sm:px-6 sm:text-xl">
          Chủ đề môn học
        </h2>
        {classes.isPending ? (
          <div role="status" aria-label="Đang tải môn học phụ trách" className="space-y-2 px-4 py-3 sm:px-6">
            <Skeleton className="h-11 w-full rounded-xl" />
            <Skeleton className="h-11 w-full rounded-xl" />
          </div>
        ) : !classes.data ? (
          <div role="status" className="flex flex-wrap items-center gap-3 px-4 py-3 text-sm sm:px-6">
            <p className="text-destructive">Không thể tải môn học phụ trách.</p>
            <Button type="button" variant="outline" onClick={() => void classes.refetch()}>Thử lại</Button>
          </div>
        ) : subjects.size === 0 ? (
          <p className="px-4 py-4 text-sm text-muted-foreground sm:px-6">Bạn chưa được phân công môn học.</p>
        ) : (
          <ul className="divide-y divide-border">
            {[...subjects.values()].map((subject) => (
              <li key={subject.id} className="grid gap-3 px-4 py-3 sm:grid-cols-[minmax(0,1fr)_auto] sm:items-center sm:px-6">
                <div className="flex min-w-0 items-start gap-3">
                  <Badge variant="outline" className="shrink-0 border-academic/25 bg-academic-soft text-academic">{subject.code}</Badge>
                  <span className="min-w-0 wrap-anywhere text-sm font-medium">{subject.name}</span>
                </div>
                <Button type="button" variant="outline" className="min-h-11 justify-self-start"
                  aria-label={`Quản lý chủ đề môn ${subject.name} (${subject.code})`}
                  onClick={() => setManagingSubjectId(subject.id)}>
                  Quản lý chủ đề
                </Button>
              </li>
            ))}
          </ul>
        )}
      </section>

      {managingSubject && (
        <TopicCatalogDialog key={managingSubject.id} subjectId={managingSubject.id}
          subjectName={managingSubject.name} onClose={() => setManagingSubjectId(undefined)} />
      )}

      <LecturerQuestionsExportCard />

      <section aria-labelledby="lecturer-classes-title" className="space-y-3">
        <div className="flex flex-wrap items-center gap-2">
          <h2 id="lecturer-classes-title" className="text-xl font-semibold">Lớp học phần phụ trách</h2>
          {classes.data && <Badge variant="secondary">{classes.data.length} lớp</Badge>}
        </div>
        {classes.isPending ? (
          <div role="status" aria-label="Đang tải lớp học phần" className="grid gap-3 sm:grid-cols-2 xl:grid-cols-3">
            {[1, 2, 3].map((key) => <Skeleton key={key} className="h-40 rounded-2xl" />)}
          </div>
        ) : !classes.data ? (
          <ErrorState title="Không thể tải lớp học phần" onRetry={() => void classes.refetch()} />
        ) : classes.data.length === 0 ? (
          <EmptyState icon={Layers} title="Bạn chưa được phân công lớp học phần" />
        ) : (
          <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-3">
            {classes.data.map((courseClass) => (
              <article key={courseClass.courseClassId} className="flex min-w-0 flex-col rounded-2xl border border-border bg-card p-4 text-card-foreground shadow-[var(--card-shadow)]">
                <div className="mb-2 flex flex-wrap items-center gap-2">
                  <Badge variant="outline" className="border-academic/25 bg-academic-soft text-academic">{courseClass.subjectCode}</Badge>
                  <span className="text-xs text-muted-foreground">{courseClass.activeSemester ? 'Học kỳ hiện tại' : 'Lịch sử'}</span>
                </div>
                <h3 className="wrap-break-word text-base font-semibold">{courseClass.subjectName}</h3>
                <p className="mt-1 grow text-sm text-muted-foreground">
                  Lớp {courseClass.classCode} · Học kỳ {courseClass.semesterName}
                </p>
                <div className="mt-3 flex flex-wrap items-center justify-between gap-x-2 border-t border-border pt-1">
                  <span className="flex items-center gap-1.5 text-sm text-muted-foreground">
                    <Users aria-hidden="true" className="size-4" /> {courseClass.totalStudents ?? 0} sinh viên
                  </span>
                  {courseClass.activeSemester && (
                    <Button nativeButton={false} variant="ghost" className="text-academic" render={
                      <Link to={`/lecturer/exams?classId=${courseClass.courseClassId}`} aria-label={`Tạo đề cho lớp ${courseClass.classCode}`} />
                    }>
                      Tạo đề <ArrowRight aria-hidden="true" />
                    </Button>
                  )}
                </div>
              </article>
            ))}
          </div>
        )}
      </section>
    </div>
  );
}
