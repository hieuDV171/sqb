import { useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { Calendar, Download, Eye, FileText, GraduationCap, Plus, RefreshCw } from 'lucide-react';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { CursorPagination, type PageCursor } from '@/components/ui/cursor-pagination';
import { EmptyState } from '@/components/ui/empty-state';
import { ErrorState } from '@/components/ui/error-state';
import { FilterSelect } from '@/components/ui/filter-select';
import { Skeleton } from '@/components/ui/skeleton';
import { ExamDetailModal, ExportExamModal, GenerateExamModal, useLecturerClasses, useMyExams } from '@/features/exam';
import { ExamDifficultySummary } from '@/features/exam/components/ExamDifficultySummary';
import { getExamSubjects } from '@/features/exam/examPresentation';

export function LecturerExamsPage() {
  const [params, setParams] = useSearchParams();
  const classes = useLecturerClasses();
  const subjects = getExamSubjects(classes.data);
  const requestedClassId = Number(params.get('classId'));
  const initialClassId = Number.isSafeInteger(requestedClassId) && requestedClassId > 0 ? requestedClassId : undefined;

  const [selectedSubjectId, setSelectedSubjectId] = useState<number>();
  const [cursor, setCursor] = useState<PageCursor>({});
  const [isGenerateOpen, setIsGenerateOpen] = useState(false);
  const [previewExamId, setPreviewExamId] = useState<number | null>(null);
  const [exportExam, setExportExam] = useState<{ id: number; title: string } | null>(null);
  const examsQuery = useMyExams({ subjectId: selectedSubjectId, ...cursor, limit: 10 });
  const exams = examsQuery.data?.items ?? [];
  const canGenerate = Boolean(classes.data?.length);

  function closeGenerate() {
    setIsGenerateOpen(false);
    if (params.has('classId')) {
      const next = new URLSearchParams(params);
      next.delete('classId');
      setParams(next, { replace: true });
    }
  }

  return (
    <div className="mx-auto max-w-6xl space-y-6 pb-8">
      <header className="flex flex-wrap items-end justify-between gap-4 border-b border-border pb-5">
        <div className="space-y-2">
          <p className="flex items-center gap-2 text-sm font-semibold text-academic">
            <FileText aria-hidden="true" className="size-4" /> Không gian giảng viên
          </p>
          <h1 className="text-2xl font-bold tracking-tight sm:text-3xl">Quản lý đề thi</h1>
          <p className="text-sm text-muted-foreground">Tạo đề từ câu hỏi đã duyệt, soát đề và xuất tệp PDF hoặc Excel.</p>
        </div>
        <Button onClick={() => setIsGenerateOpen(true)} disabled={!canGenerate}>
          <Plus aria-hidden="true" /> Tạo đề thi
        </Button>
      </header>

      <section aria-label="Bộ lọc đề thi" className="space-y-3 rounded-2xl border border-border bg-card p-4 text-card-foreground shadow-[var(--card-shadow)]">
        <div className="grid items-end gap-3 sm:grid-cols-[minmax(0,25rem)_auto] sm:justify-between">
          <FilterSelect label="Môn học" value={selectedSubjectId?.toString() ?? ''}
            disabled={subjects.length === 0}
            options={[{ value: '', label: 'Tất cả môn học' }, ...subjects.map((subject) => ({ value: String(subject.id), label: subject.label }))]}
            onChange={(value) => {
              setSelectedSubjectId(value ? Number(value) : undefined);
              setCursor({});
            }} />
          <div className="flex flex-wrap items-center gap-3 sm:justify-end">
            {examsQuery.data && <p role="status" className="text-sm text-muted-foreground">{exams.length} đề thi trên trang này</p>}
            <Button variant="outline" onClick={() => void examsQuery.refetch()} disabled={examsQuery.isFetching}>
              <RefreshCw aria-hidden="true" className={examsQuery.isFetching ? 'motion-safe:animate-spin' : undefined} /> Làm mới
            </Button>
          </div>
        </div>
        {classes.isPending ? (
          <p role="status" className="text-sm text-muted-foreground">Đang tải môn học phụ trách…</p>
        ) : !classes.data ? (
          <div className="flex flex-wrap items-center gap-2" role="status">
            <p className="text-sm text-destructive">Không thể tải môn học phụ trách.</p>
            <Button variant="ghost" onClick={() => void classes.refetch()}>Thử lại</Button>
          </div>
        ) : !canGenerate && (
          <p className="text-sm text-muted-foreground">Cần có lớp học phần trong học kỳ hiện tại để tạo đề.</p>
        )}
      </section>

      {examsQuery.isPending ? (
        <div role="status" aria-label="Đang tải đề thi" className="grid gap-3 lg:grid-cols-2">
          {[1, 2, 3, 4].map((key) => <Skeleton key={key} className="h-60 rounded-2xl" />)}
        </div>
      ) : !examsQuery.data ? (
        <ErrorState title="Không thể tải danh sách đề thi" onRetry={() => void examsQuery.refetch()} />
      ) : (
        <>
          {examsQuery.isRefetchError && <p role="status" className="text-sm text-destructive">Không thể làm mới danh sách đề thi. Dùng nút Làm mới để thử lại.</p>}
          {exams.length === 0 ? (
            <EmptyState icon={FileText}
              title={selectedSubjectId ? 'Môn này chưa có đề thi' : 'Chưa có đề thi'}
              description={canGenerate ? 'Tạo đề từ ngân hàng câu hỏi đã duyệt.' : undefined}
              actionIcon={Plus} actionLabel={canGenerate ? 'Tạo đề thi' : undefined}
              onAction={() => setIsGenerateOpen(true)} />
          ) : (
            <section aria-label="Danh sách đề thi" className="grid gap-3 lg:grid-cols-2">
              {exams.map((exam) => {
                const title = exam.title || `Đề thi #${exam.examId}`;
                return (
                  <article key={exam.examId} className="flex min-w-0 flex-col gap-3 rounded-2xl border border-border bg-card p-4 text-card-foreground shadow-[var(--card-shadow)]">
                    <div className="flex flex-wrap items-center gap-2">
                      {exam.subjectCode && <Badge variant="outline" className="border-academic/25 bg-academic-soft text-academic">{exam.subjectCode}</Badge>}
                      <Badge variant="secondary">{exam.questionCount} câu hỏi</Badge>
                    </div>
                    <div className="grow space-y-1.5">
                      <h2 className="wrap-break-word text-lg font-semibold leading-snug">{title}</h2>
                      <div className="flex flex-wrap gap-x-4 gap-y-1 text-sm text-muted-foreground">
                        {exam.subjectName && <span className="flex min-w-0 items-center gap-1.5"><GraduationCap aria-hidden="true" className="size-4 shrink-0" />{exam.subjectName}</span>}
                        {exam.createdAt && <span className="flex items-center gap-1.5"><Calendar aria-hidden="true" className="size-4" /><time dateTime={exam.createdAt}>{new Date(exam.createdAt).toLocaleDateString('vi-VN')}</time></span>}
                      </div>
                    </div>
                    {exam.statistic && <ExamDifficultySummary statistic={exam.statistic} />}
                    <div className="flex flex-wrap justify-end gap-2 border-t border-border pt-3">
                      <Button variant="outline" onClick={() => setPreviewExamId(exam.examId)}><Eye aria-hidden="true" /> Soát đề</Button>
                      <Button onClick={() => setExportExam({ id: exam.examId, title })}><Download aria-hidden="true" /> Xuất tệp</Button>
                    </div>
                  </article>
                );
              })}
            </section>
          )}
          <CursorPagination pagination={examsQuery.data.pagination} cursor={cursor} busy={examsQuery.isFetching} onChange={setCursor} />
        </>
      )}

      {(isGenerateOpen || initialClassId !== undefined) && (
        <GenerateExamModal initialClassId={initialClassId} initialSubjectId={selectedSubjectId} onClose={closeGenerate}
          onSuccess={(id) => {
            closeGenerate();
            setCursor({});
            setPreviewExamId(id);
          }} />
      )}
      {previewExamId !== null && <ExamDetailModal examId={previewExamId} onClose={() => setPreviewExamId(null)} />}
      {exportExam && <ExportExamModal examId={exportExam.id} examTitle={exportExam.title} onClose={() => setExportExam(null)} />}
    </div>
  );
}
