import React from 'react';
import { Check, X, Award } from 'lucide-react';
import { cn } from '@/lib/utils';
import type { PracticeQuestionDto } from '../types/practice.types';

interface PracticeProgressBarProps {
  questions: PracticeQuestionDto[];
  currentIndex: number;
  onSelectQuestion: (index: number) => void;
  userAnswersRecord: Record<number, { isCorrect: boolean; selected: string[] }>;
  onOpenSummary?: () => void;
}

export const PracticeProgressBar: React.FC<PracticeProgressBarProps> = ({
  questions,
  currentIndex,
  onSelectQuestion,
  userAnswersRecord,
  onOpenSummary,
}) => {
  const total = questions.length;
  if (total === 0) return null;

  // Count answered, correct, incorrect
  let answeredCount = 0;
  let correctCount = 0;
  let incorrectCount = 0;

  questions.forEach((q) => {
    const local = userAnswersRecord[q.questionId];
    if (local) {
      answeredCount++;
      if (local.isCorrect) correctCount++;
      else incorrectCount++;
    } else if (q.myInteraction.answered) {
      answeredCount++;
    }
  });

  const progressPercent = Math.round((answeredCount / total) * 100);

  return (
    <div className="p-4 sm:p-5 rounded-3xl bg-white dark:bg-slate-900 border border-slate-200/80 dark:border-slate-800 shadow-xs space-y-4">
      {/* Top Header Row */}
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div className="flex items-center gap-3">
          <div className="flex items-center gap-2">
            <span className="font-bold text-slate-800 dark:text-slate-100 text-xs sm:text-sm">
              Tiến độ làm bài:
            </span>
            <span className="px-2.5 py-0.5 rounded-full text-xs font-black bg-indigo-50 dark:bg-indigo-950/80 text-indigo-600 dark:text-indigo-400">
              {answeredCount}/{total} câu ({progressPercent}%)
            </span>
          </div>

          {/* Correct / Incorrect Badges */}
          {(correctCount > 0 || incorrectCount > 0) && (
            <div className="hidden sm:flex items-center gap-2 text-xs">
              <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-lg bg-emerald-50 dark:bg-emerald-950/40 text-emerald-600 dark:text-emerald-400 font-bold border border-emerald-200 dark:border-emerald-800">
                <Check className="w-3.5 h-3.5" /> {correctCount} đúng
              </span>
              <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-lg bg-rose-50 dark:bg-rose-950/40 text-rose-600 dark:text-rose-400 font-bold border border-rose-200 dark:border-rose-800">
                <X className="w-3.5 h-3.5" /> {incorrectCount} sai
              </span>
            </div>
          )}
        </div>

        {/* Action: Open Summary Button */}
        {answeredCount > 0 && onOpenSummary && (
          <button
            type="button"
            onClick={onOpenSummary}
            className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-amber-500/10 hover:bg-amber-500/20 text-amber-600 dark:text-amber-400 text-xs font-bold transition-all cursor-pointer"
          >
            <Award className="w-3.5 h-3.5" />
            <span>Bảng kết quả</span>
          </button>
        )}
      </div>

      {/* Progress Bar */}
      <div className="w-full h-2 bg-slate-100 dark:bg-slate-800 rounded-full overflow-hidden">
        <div
          className="h-full bg-linear-to-r from-indigo-500 to-emerald-500 transition-all duration-300 rounded-full"
          style={{ width: `${progressPercent}%` }}
        />
      </div>

      {/* Question Palette Grid */}
      <div className="flex flex-wrap items-center gap-2 pt-1">
        {questions.map((q, idx) => {
          const isCurrent = idx === currentIndex;
          const local = userAnswersRecord[q.questionId];
          const isAnswered = !!local || q.myInteraction.answered;
          const isCorrect = local ? local.isCorrect : undefined;

          let btnClass = 'bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-400 border-slate-200/80 dark:border-slate-700/80';
          if (isAnswered) {
            if (isCorrect === true) {
              btnClass = 'bg-emerald-500 text-white border-emerald-600 shadow-xs';
            } else if (isCorrect === false) {
              btnClass = 'bg-rose-500 text-white border-rose-600 shadow-xs';
            } else {
              btnClass = 'bg-indigo-500 text-white border-indigo-600 shadow-xs';
            }
          }

          return (
            <button
              key={q.questionId}
              type="button"
              onClick={() => onSelectQuestion(idx)}
              className={cn(
                'w-8 h-8 sm:w-9 sm:h-9 rounded-xl text-xs font-bold transition-all cursor-pointer border flex items-center justify-center relative active:scale-95',
                btnClass,
                isCurrent && 'ring-2 ring-indigo-500 ring-offset-2 dark:ring-offset-slate-900 scale-105 font-black z-10'
              )}
              title={`Câu ${idx + 1}`}
            >
              {idx + 1}
            </button>
          );
        })}
      </div>
    </div>
  );
};
