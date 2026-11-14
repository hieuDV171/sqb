import { ArrowRight, ClipboardCheck, FileText, Layers, Users } from 'lucide-react';
import { Link } from 'react-router-dom';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { EmptyState } from '@/components/ui/empty-state';
import { ErrorState } from '@/components/ui/error-state';
import { Skeleton } from '@/components/ui/skeleton';
import { LecturerQuestionsExportCard, useLecturerClasses } from '@/features/exam';

export function LecturerQuestionsPage() {
  const classes = useLecturerClasses({ includeHistory: true });

  return (
    <div className="mx-auto max-w-6xl space-y-6 pb-8">
      <header className="flex flex-wrap items-end justify-between gap-4 border-b border-border pb-5">
        <div className="space-y-2">
          <p className="flex items-center gap-2 text-sm font-semibold text-academic">
            <Layers aria-hidden="true" className="size-4" /> Không gian giảng viên
          </p>
          <h1 className="text-2xl font-bold tracking-tight sm:text-3xl">Ngân hàng câu hỏi môn học</h1>
          <p className="text-sm text-muted-foreground">Xuất tài liệu ôn tập theo môn và xem các lớp học phần phụ trách.</p>
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
