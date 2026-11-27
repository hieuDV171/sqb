import { Dialog } from '@base-ui/react/dialog';
import { ArrowLeft, BookOpen, X } from 'lucide-react';
import { Link } from 'react-router-dom';
import { Button } from '@/components/ui/button';
import type { PracticeQuestionDto } from '../types/practice.types';

interface PracticeSummaryModalProps {
  isOpen: boolean;
  onClose: () => void;
  questions: PracticeQuestionDto[];
  userAnswersRecord: Record<number, { isCorrect: boolean; selected: string[] }>;
  onReviewFromStart: () => void;
  sessionTitle?: string;
  subjectName?: string;
  returnTo: string;
  returnLabel: string;
}

export function PracticeSummaryModal({
  isOpen, onClose, questions, userAnswersRecord, onReviewFromStart, sessionTitle, subjectName, returnTo, returnLabel,
}: PracticeSummaryModalProps) {
  let correctCount = 0;
  let incorrectCount = 0;
  let previouslyAnswered = 0;
  for (const question of questions) {
    const result = userAnswersRecord[question.questionId];
    if (result?.isCorrect === true) correctCount++;
    else if (result?.isCorrect === false) incorrectCount++;
    else if (question.myInteraction.answered) previouslyAnswered++;
  }
  const answeredCount = correctCount + incorrectCount + previouslyAnswered;

  return (
    <Dialog.Root open={isOpen} onOpenChange={(open) => { if (!open) onClose(); }}>
      <Dialog.Portal>
        <Dialog.Backdrop className="fixed inset-0 z-50 bg-black/60" />
        <Dialog.Popup className="fixed inset-x-4 top-1/2 z-50 mx-auto max-h-[calc(100dvh-2rem)] max-w-md -translate-y-1/2 space-y-5 overflow-y-auto rounded-2xl border border-border bg-card p-5 shadow-xl sm:p-6">
          <header className="flex items-start justify-between gap-3">
            <div className="min-w-0 space-y-1">
              <Dialog.Title className="text-xl font-semibold">Kết quả luyện tập</Dialog.Title>
              <Dialog.Description className="break-words text-sm text-muted-foreground">
                {[subjectName, sessionTitle].filter(Boolean).join(' · ')}
              </Dialog.Description>
            </div>
            <Dialog.Close render={<Button variant="ghost" size="icon" className="size-11" aria-label="Đóng kết quả" />}><X aria-hidden="true" /></Dialog.Close>
          </header>
          <dl className="grid grid-cols-2 gap-2">
            {[
              { label: 'Đã trả lời', value: `${answeredCount}/${questions.length}` },
              { label: 'Đúng lần này', value: correctCount },
              { label: 'Sai lần này', value: incorrectCount },
              { label: 'Đã làm trước đó', value: previouslyAnswered },
            ].map(({ label, value }) => (
              <div key={label} className="space-y-1 rounded-xl border border-border bg-surface-subtle p-3">
                <dt className="text-sm text-muted-foreground">{label}</dt>
                <dd className="text-xl font-bold tabular-nums">{value}</dd>
              </div>
            ))}
          </dl>
          <div className="grid gap-2">
            <Button className="min-h-11" onClick={onReviewFromStart}><BookOpen aria-hidden="true" /> Xem lại từ câu đầu</Button>
            <Button variant="ghost" className="min-h-11" render={<Link to={returnTo} />} nativeButton={false}>
              <ArrowLeft aria-hidden="true" /> {returnLabel}
            </Button>
          </div>
        </Dialog.Popup>
      </Dialog.Portal>
    </Dialog.Root>
  );
}
