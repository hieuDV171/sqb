import { useState } from 'react';
import { ArrowRight, ChevronLeft, ChevronRight, ClipboardCheck, FileText, Layers, Search, Users } from 'lucide-react';
import { Link } from 'react-router-dom';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { EmptyState } from '@/components/ui/empty-state';
import { ErrorState } from '@/components/ui/error-state';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Skeleton } from '@/components/ui/skeleton';
import { LecturerQuestionsExportCard, useLecturerClasses } from '@/features/exam';
import { TopicCatalogDialog } from '@/features/session/components/TopicCatalogDialog';

interface AssignedSubject {
  id: number;
  code: string;
  name: string;
  activeSemester: boolean;
}

const SUBJECTS_PER_PAGE = 5;
const CLASS_PREVIEW_LIMIT = 3;

function normalizeSubjectSearch(value: string) {
  return value.toLocaleLowerCase('vi').normalize('NFD').replace(/\p{M}/gu, '').replace(/đ/g, 'd').trim();
}

export function LecturerQuestionsPage() {
  const classes = useLecturerClasses({ includeHistory: true });
  const [managingSubjectId, setManagingSubjectId] = useState<number>();
  const [subjectSearch, setSubjectSearch] = useState('');
  const [subjectPage, setSubjectPage] = useState(1);
  const subjects = new Map<number, AssignedSubject>();
  for (const { subjectId, subjectCode, subjectName, activeSemester } of classes.data ?? []) {
    if (!subjects.has(subjectId) || activeSemester) {
      subjects.set(subjectId, { id: subjectId, code: subjectCode, name: subjectName, activeSemester });
    }
  }
  const managingSubject = managingSubjectId === undefined ? undefined : subjects.get(managingSubjectId);
  const searchTerm = normalizeSubjectSearch(subjectSearch);
  const filteredSubjects = [...subjects.values()]
    .filter((subject) => !searchTerm || normalizeSubjectSearch(subject.code).includes(searchTerm)
      || normalizeSubjectSearch(subject.name).includes(searchTerm))
    .sort((left, right) => Number(right.activeSemester) - Number(left.activeSemester) || left.code.localeCompare(right.code, 'vi'));
  const pageCount = Math.max(1, Math.ceil(filteredSubjects.length / SUBJECTS_PER_PAGE));
  const page = Math.min(subjectPage, pageCount);
  const visibleSubjects = filteredSubjects.slice((page - 1) * SUBJECTS_PER_PAGE, page * SUBJECTS_PER_PAGE);
  const currentClasses = (classes.data ?? []).filter((courseClass) => courseClass.activeSemester);

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
          <Button nativeButton={false} variant="outline" render={<Link to="/lecturer/sessions" />}>
            <ClipboardCheck aria-hidden="true" /> Duyệt câu hỏi
          </Button>
          <Button nativeButton={false} render={<Link to="/lecturer/exams" />}>
            <FileText aria-hidden="true" /> Quản lý đề thi
          </Button>
        </div>
      </header>

      <LecturerQuestionsExportCard />

      <section aria-labelledby="lecturer-topics-title" className="overflow-hidden rounded-2xl border border-border bg-card text-card-foreground shadow-[var(--card-shadow)]">
        <div className="space-y-3 border-b border-border px-4 py-3 sm:px-6">
          <h2 id="lecturer-topics-title" className="text-lg font-semibold sm:text-xl">Chủ đề môn học</h2>
          {subjects.size > 0 && (
            <div className="max-w-md space-y-1.5">
              <Label htmlFor="lecturer-topic-subject-search">Tìm môn học</Label>
              <div className="relative">
                <Search aria-hidden="true" className="pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2 text-muted-foreground" />
                <Input id="lecturer-topic-subject-search" type="search" placeholder="Nhập mã hoặc tên môn" className="min-h-11 pl-9"
                  value={subjectSearch}
                  onChange={(event) => {
                    setSubjectSearch(event.target.value);
                    setSubjectPage(1);
                  }} />
              </div>
            </div>
          )}
        </div>
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
        ) : filteredSubjects.length === 0 ? (
          <p role="status" className="px-4 py-4 text-sm text-muted-foreground sm:px-6">Không tìm thấy môn học phù hợp.</p>
        ) : (
          <>
            <ul className="divide-y divide-border">
              {visibleSubjects.map((subject) => (
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
            {pageCount > 1 && (
              <nav aria-label="Phân trang môn học" className="flex flex-wrap items-center justify-between gap-2 border-t border-border px-4 py-3 sm:px-6">
                <span role="status" className="text-sm text-muted-foreground">Trang {page}/{pageCount} · {filteredSubjects.length} môn</span>
                <div className="flex flex-wrap gap-2">
                  <Button type="button" variant="outline" className="min-h-11" disabled={page === 1} onClick={() => setSubjectPage(page - 1)}>
                    <ChevronLeft aria-hidden="true" /> Trang trước
                  </Button>
                  <Button type="button" variant="outline" className="min-h-11" disabled={page === pageCount} onClick={() => setSubjectPage(page + 1)}>
                    Trang sau <ChevronRight aria-hidden="true" />
                  </Button>
                </div>
              </nav>
            )}
          </>
        )}
      </section>

      {managingSubject && (
        <TopicCatalogDialog key={managingSubject.id} subjectId={managingSubject.id}
          subjectName={managingSubject.name} onClose={() => setManagingSubjectId(undefined)} />
      )}

      <section aria-labelledby="lecturer-classes-title" className="space-y-3">
        <div className="flex flex-wrap items-center justify-between gap-2">
          <div className="flex flex-wrap items-center gap-2">
            <h2 id="lecturer-classes-title" className="text-xl font-semibold">Lớp học phần phụ trách</h2>
            {classes.data && <Badge variant="secondary">{currentClasses.length} lớp kỳ này</Badge>}
          </div>
          <Button nativeButton={false} variant="outline" className="min-h-11" render={<Link to="/lecturer/course-classes" />}>
            Xem tất cả lớp <ArrowRight aria-hidden="true" />
          </Button>
        </div>
        {classes.isPending ? (
          <div role="status" aria-label="Đang tải lớp học phần" className="grid gap-3 sm:grid-cols-2 xl:grid-cols-3">
            {[1, 2, 3].map((key) => <Skeleton key={key} className="h-40 rounded-2xl" />)}
          </div>
        ) : !classes.data ? (
          <ErrorState title="Không thể tải lớp học phần" onRetry={() => void classes.refetch()} />
        ) : currentClasses.length === 0 ? (
          <EmptyState icon={Layers} title="Bạn chưa có lớp học phần trong học kỳ hiện tại" />
        ) : (
          <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-3">
            {currentClasses.slice(0, CLASS_PREVIEW_LIMIT).map((courseClass) => (
              <article key={courseClass.courseClassId} className="flex min-w-0 flex-col rounded-2xl border border-border bg-card p-4 text-card-foreground shadow-[var(--card-shadow)]">
                <div className="mb-2 flex flex-wrap items-center gap-2">
                  <Badge variant="outline" className="border-academic/25 bg-academic-soft text-academic">{courseClass.subjectCode}</Badge>
                </div>
                <h3 className="wrap-break-word text-base font-semibold">{courseClass.subjectName}</h3>
                <p className="mt-1 grow text-sm text-muted-foreground">
                  Lớp {courseClass.classCode} · Học kỳ {courseClass.semesterName}
                </p>
                <div className="mt-3 flex flex-wrap items-center justify-between gap-x-2 border-t border-border pt-1">
                  <span className="flex items-center gap-1.5 text-sm text-muted-foreground">
                    <Users aria-hidden="true" className="size-4" /> {courseClass.totalStudents ?? 0} sinh viên
                  </span>
                  <Button nativeButton={false} variant="ghost" className="text-academic" render={
                    <Link to={`/lecturer/exams?classId=${courseClass.courseClassId}`} aria-label={`Tạo đề cho lớp ${courseClass.classCode}`} />
                  }>
                    Tạo đề <ArrowRight aria-hidden="true" />
                  </Button>
                </div>
              </article>
            ))}
          </div>
        )}
      </section>
    </div>
  );
}
