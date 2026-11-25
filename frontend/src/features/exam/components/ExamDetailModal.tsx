import { useState } from 'react';
import { Check, ClipboardList, Download } from 'lucide-react';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { EmptyState } from '@/components/ui/empty-state';
import { ErrorState } from '@/components/ui/error-state';
import { Skeleton } from '@/components/ui/skeleton';
import { cn } from '@/lib/utils';
import { examDifficulties, examPercentFormat } from '../examPresentation';
import { useExamDetail } from '../hooks/useExam';
import { ExamDialog } from './ExamDialog';
import { ExamDifficultySummary } from './ExamDifficultySummary';
import { ExportExamModal } from './ExportExamModal';

interface ExamDetailModalProps {
  examId: number;
  onClose: () => void;
}

export function ExamDetailModal({ examId, onClose }: ExamDetailModalProps) {
  const [isExportOpen, setIsExportOpen] = useState(false);
  const query = useExamDetail(examId);
  const exam = query.data;

  return (
    <ExamDialog title={exam?.title || `Đề thi #${examId}`}
      description={exam ? [exam.subjectName, `${exam.questionCount} câu hỏi`].filter(Boolean).join(' · ') : 'Soát đề thi'}
      icon={ClipboardList} className="max-w-3xl" onClose={onClose}>
      <div className="min-h-0 space-y-4 overflow-y-auto p-4 sm:p-5">
        {query.isPending ? (
          <div role="status" aria-label="Đang tải đề thi" className="space-y-3">
            {[1, 2, 3].map((key) => <Skeleton key={key} className="h-32 rounded-xl" />)}
          </div>
        ) : !exam ? (
          <ErrorState title="Không thể tải chi tiết đề thi" onRetry={() => void query.refetch()} />
        ) : (
          <>
            {exam.statistic && <ExamDifficultySummary statistic={exam.statistic} requested={exam.difficultyMatrix?.requested} />}
            {!!exam.topicMatrix?.rows.length && (
              <section aria-label="Ma trận chủ đề" className="space-y-2 rounded-xl bg-surface-subtle p-3">
                <h3 className="text-sm font-medium">Ma trận chủ đề</h3>
                <ul className="divide-y divide-border">
                  {exam.topicMatrix.rows.map((row) => (
                    <li key={row.topicId ?? 'untagged'} className="space-y-2 py-2 text-sm">
                      <div className="flex flex-wrap items-baseline justify-between gap-x-3">
                        <p className="font-medium wrap-break-word">{row.topicName}</p>
                        <p className="text-xs text-muted-foreground">
                          {row.requestedCount != null && row.requestedWeight != null && (
                            <>Yêu cầu {examPercentFormat.format(row.requestedWeight)} ({row.requestedCount} câu) · </>
                          )}
                          <strong className="font-semibold text-foreground">{row.actualCount} câu thực tế</strong>
                        </p>
                      </div>
                      <dl className="flex flex-wrap gap-1.5 text-xs">
                        {examDifficulties.map(({ field, count, label, tone }) => (
                          <div key={field} className={cn('flex gap-1.5 rounded-md px-2 py-0.5', tone)}>
                            <dt>{label}</dt>
                            <dd className="font-semibold tabular-nums">{row.actualByDifficulty[count] ?? 0}</dd>
                          </div>
                        ))}
                      </dl>
                    </li>
                  ))}
                </ul>
              </section>
            )}
            {exam.questions.length === 0 ? (
              <EmptyState icon={ClipboardList} title="Đề thi chưa có câu hỏi" />
            ) : (
              <ol className="space-y-3">
                {exam.questions.map((question, index) => {
                  const difficulty = examDifficulties.find((item) => item.value === question.difficulty) ?? examDifficulties[3];
                  return (
                    <li key={question.questionId} className="space-y-3 rounded-xl border border-border bg-surface-subtle p-4">
                      <div className="flex flex-wrap items-center gap-2">
                        <span className="text-sm font-semibold">Câu {question.order || index + 1}</span>
                        <Badge variant="outline" className={difficulty.tone}>{difficulty.label}</Badge>
                        {question.topic && <span className="text-sm text-muted-foreground">{question.topic}</span>}
                      </div>
                      <p className="whitespace-pre-wrap wrap-break-word text-sm leading-relaxed">{question.content}</p>
                      {Boolean(question.imageUrls?.length) && (
                        <div className="flex flex-wrap gap-2">
                          {question.imageUrls?.map((url, imageIndex) => (
                            <img key={url} src={url} alt={`Hình ${imageIndex + 1} của câu hỏi`} loading="lazy"
                              className="max-h-48 max-w-full rounded-lg border border-border object-contain" />
                          ))}
                        </div>
                      )}
                      <ul className="grid gap-2 sm:grid-cols-2">
                        {question.options.map((option) => (
                          <li key={option.key} className={cn(
                            'flex min-w-0 items-start gap-2 rounded-lg border border-border bg-card p-3 text-sm',
                            option.isCorrect && 'border-success/35 bg-success-soft',
                          )}>
                            <span className="shrink-0 font-semibold">{option.key}.</span>
                            <div className="min-w-0 grow space-y-1 wrap-break-word">
                              {option.text}
                              {option.mediaUrl && <img src={option.mediaUrl} alt={`Hình phương án ${option.key}`} loading="lazy" className="max-h-32 max-w-full rounded-lg object-contain" />}
                              {option.isCorrect && <span className="flex items-center gap-1 text-xs font-semibold text-success"><Check aria-hidden="true" className="size-3" /> Đáp án đúng</span>}
                            </div>
                          </li>
                        ))}
                      </ul>
                    </li>
                  );
                })}
              </ol>
            )}
          </>
        )}
      </div>
      {exam && (
        <footer className="flex shrink-0 justify-end border-t border-border p-4 sm:px-5">
          <Button onClick={() => setIsExportOpen(true)}><Download aria-hidden="true" /> Xuất tệp</Button>
        </footer>
      )}
      {isExportOpen && <ExportExamModal examId={examId} examTitle={exam?.title} onClose={() => setIsExportOpen(false)} />}
    </ExamDialog>
  );
}
