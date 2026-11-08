import { Check, X } from 'lucide-react';
import { cn } from '@/lib/utils';
import type { PracticeQuestionDto } from '../types/practice.types';

interface PracticeProgressBarProps {
  questions: PracticeQuestionDto[];
  currentIndex: number;
  onSelectQuestion: (index: number) => void;
  userAnswersRecord: Record<number, { isCorrect: boolean; selected: string[] }>;
}

export function PracticeProgressBar({ questions, currentIndex, onSelectQuestion, userAnswersRecord }: PracticeProgressBarProps) {
  const answeredCount = questions.filter((q) => q.myInteraction.answered || userAnswersRecord[q.questionId]).length;

  return (
    <aside aria-labelledby="practice-progress" className="space-y-4 rounded-2xl border border-border bg-card p-4">
      <div className="space-y-2">
        <h2 id="practice-progress" className="font-semibold">Tiến độ</h2>
        <p className="text-sm text-muted-foreground" role="status">Đã trả lời {answeredCount}/{questions.length} câu</p>
        <progress aria-label="Số câu đã trả lời" className="block h-2 w-full accent-primary" max={questions.length} value={answeredCount} />
      </div>
      <nav aria-label="Chọn câu hỏi" className="flex flex-wrap gap-2">
        {questions.map(({ questionId, myInteraction, hiddenFields }, index) => {
          const correct = userAnswersRecord[questionId]?.isCorrect ?? hiddenFields?.wasCorrect ?? undefined;
          const status = correct === true ? 'Đúng' : correct === false ? 'Sai' : myInteraction.answered ? 'Đã trả lời trước đó' : 'Chưa trả lời';
          return (
            <button
              key={questionId}
              type="button"
              aria-label={`Câu ${index + 1} · ${status}`}
              aria-current={index === currentIndex ? 'step' : undefined}
              onClick={() => onSelectQuestion(index)}
              className={cn(
                'inline-flex size-11 cursor-pointer items-center justify-center gap-0.5 rounded-lg border border-border bg-background text-xs font-semibold tabular-nums hover:bg-muted focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-ring',
                myInteraction.answered && 'bg-academic-soft text-academic',
                correct === true && 'border-success bg-success-soft text-success',
                correct === false && 'border-destructive bg-destructive/10 text-destructive',
                index === currentIndex && 'ring-2 ring-ring ring-offset-1 ring-offset-card',
              )}
            >
              {index + 1}
              {correct === true && <Check aria-hidden="true" className="size-3" />}
              {correct === false && <X aria-hidden="true" className="size-3" />}
              {correct === undefined && myInteraction.answered && <span aria-hidden="true">•</span>}
            </button>
          );
        })}
      </nav>
    </aside>
  );
}
