import { useId, useState } from 'react';
import { BookOpen, Download, Loader2 } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { FilterSelect } from '@/components/ui/filter-select';
import { Skeleton } from '@/components/ui/skeleton';
import { getExamSubjects } from '../examPresentation';
import { useExportQuestions, useLecturerClasses } from '../hooks/useExam';

export function LecturerQuestionsExportCard() {
  const classes = useLecturerClasses();
  const exportMutation = useExportQuestions();
  const formatName = useId();
  const [subjectId, setSubjectId] = useState<number>();
  const [format, setFormat] = useState<'pdf' | 'excel'>('pdf');
  const [includeAnswer, setIncludeAnswer] = useState(true);
  const subjects = getExamSubjects(classes.data);
  const selectedSubject = subjects.find((subject) => subject.id === subjectId) ?? subjects[0];
  const lastExport = exportMutation.data?.data;

  return (
    <section aria-labelledby="questions-export-title" className="space-y-5 rounded-2xl border border-border bg-card p-4 text-card-foreground shadow-[var(--card-shadow)] sm:p-6">
      <header className="flex items-start gap-3">
        <span className="grid size-11 shrink-0 place-items-center rounded-xl bg-academic-soft text-academic">
          <BookOpen aria-hidden="true" className="size-5" />
        </span>
        <div>
          <h2 id="questions-export-title" className="text-lg font-semibold sm:text-xl">Xuất tài liệu ôn tập</h2>
          <p className="mt-1 text-sm text-muted-foreground">Tải toàn bộ câu hỏi của môn, gồm cả câu chưa duyệt và bị từ chối, dưới dạng PDF hoặc Excel.</p>
        </div>
      </header>
      {classes.isPending ? (
        <div role="status" aria-label="Đang tải môn học" className="grid gap-3 lg:grid-cols-3">
          {[1, 2, 3].map((key) => <Skeleton key={key} className="h-16 rounded-xl" />)}
        </div>
      ) : !classes.data ? (
        <div className="flex flex-wrap items-center gap-2" role="status">
          <p className="text-sm text-destructive">Không thể tải môn học phụ trách.</p>
          <Button variant="outline" onClick={() => void classes.refetch()}>Thử lại</Button>
        </div>
      ) : !selectedSubject ? (
        <p className="rounded-xl bg-surface-subtle p-3 text-sm text-muted-foreground">
          Chưa có môn học phụ trách trong học kỳ hiện tại.
        </p>
      ) : (
        <form onSubmit={(event) => {
          event.preventDefault();
          if (!exportMutation.isPending) {
            exportMutation.mutate({ subjectId: selectedSubject.id, format, includeAnswer });
          }
        }}>
          <fieldset disabled={exportMutation.isPending} className="grid min-w-0 items-end gap-4 lg:grid-cols-[minmax(0,1.4fr)_minmax(0,0.8fr)_minmax(0,1fr)]">
            <FilterSelect label="Môn học phụ trách" value={String(selectedSubject.id)}
              disabled={exportMutation.isPending}
              options={subjects.map((subject) => ({ value: String(subject.id), label: subject.label }))}
              onChange={(value) => setSubjectId(Number(value))} />
            <fieldset className="min-w-0">
              <legend className="mb-1.5 text-sm font-medium">Định dạng</legend>
              <div className="grid grid-cols-2 gap-2">
                {(['pdf', 'excel'] as const).map((value) => (
                  <label key={value} className="flex min-h-11 cursor-pointer items-center justify-center gap-2 rounded-xl border border-input px-2 text-sm font-medium has-checked:border-academic has-checked:bg-academic-soft has-checked:text-academic">
                    <input type="radio" name={formatName} checked={format === value} onChange={() => setFormat(value)} className="size-4 shrink-0 accent-academic" />
                    {value === 'pdf' ? 'PDF' : 'Excel'}
                  </label>
                ))}
              </div>
            </fieldset>
            <label className="flex min-h-11 cursor-pointer items-center gap-2 self-end rounded-xl border border-input px-3 py-2 text-sm">
              <input type="checkbox" checked={includeAnswer} onChange={(event) => setIncludeAnswer(event.target.checked)} className="size-4 shrink-0 accent-academic" />
              Kèm đáp án và lời giải
            </label>
          </fieldset>
          <div className="mt-5 flex flex-wrap items-center justify-end gap-3 border-t border-border pt-4">
            {exportMutation.isError && <p role="alert" className="mr-auto text-sm text-destructive">{exportMutation.error.message || 'Không thể xuất tài liệu ôn tập.'}</p>}
            {exportMutation.isSuccess && !lastExport?.downloadUrl && (
              <p role="alert" className="mr-auto text-sm text-destructive">Không nhận được liên kết tải tệp.</p>
            )}
            {lastExport?.downloadUrl && (
              <p role="status" className="min-w-0 grow wrap-anywhere text-sm text-success">
                Đã xuất {lastExport.fileName || 'tài liệu ôn tập'}.{' '}
                <a className="font-semibold underline underline-offset-2 focus-visible:outline-2 focus-visible:outline-ring"
                  href={lastExport.downloadUrl} download referrerPolicy="no-referrer">
                  Tải lại
                </a>
              </p>
            )}
            <Button type="submit" disabled={exportMutation.isPending}>
              {exportMutation.isPending ? <Loader2 aria-hidden="true" className="motion-safe:animate-spin" /> : <Download aria-hidden="true" />}
              {exportMutation.isPending ? 'Đang xuất…' : 'Xuất tệp'}
            </Button>
          </div>
        </form>
      )}
    </section>
  );
}
