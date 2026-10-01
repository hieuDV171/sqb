import React from 'react';
import { useNavigate } from 'react-router-dom';
import { RotateCcw, ArrowLeft, Trophy, BookOpen } from 'lucide-react';
import type { PracticeQuestionDto } from '../types/practice.types';

interface PracticeSummaryModalProps {
  isOpen: boolean;
  onClose: () => void;
  questions: PracticeQuestionDto[];
  userAnswersRecord: Record<number, { isCorrect: boolean; selected: string[] }>;
  onRestartPractice: () => void;
  sessionTitle?: string;
  subjectName?: string;
}

export const PracticeSummaryModal: React.FC<PracticeSummaryModalProps> = ({
  isOpen,
  onClose,
  questions,
  userAnswersRecord,
  onRestartPractice,
  sessionTitle,
  subjectName,
}) => {
  const navigate = useNavigate();

  if (!isOpen) return null;

  const total = questions.length;
  let correctCount = 0;
  let incorrectCount = 0;
  let answeredCount = 0;

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

  const accuracy = total > 0 ? Math.round((correctCount / total) * 100) : 0;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/70 backdrop-blur-xs animate-in fade-in duration-200">
      <div className="relative w-full max-w-md rounded-3xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 shadow-2xl p-6 sm:p-8 space-y-6 text-center">
        {/* Top Trophy / Badge Icon */}
        <div className="w-20 h-20 rounded-3xl bg-amber-500/10 text-amber-500 mx-auto flex items-center justify-center shadow-xs border border-amber-500/20">
          <Trophy className="w-10 h-10" />
        </div>

        {/* Title */}
        <div className="space-y-1.5">
          <h3 className="text-xl sm:text-2xl font-black text-slate-900 dark:text-white tracking-tight">
            Kết quả luyện tập
          </h3>
          {sessionTitle && (
            <p className="text-xs text-slate-500 dark:text-slate-400 font-medium line-clamp-1">
              {subjectName ? `[${subjectName}] ` : ''}{sessionTitle}
            </p>
          )}
        </div>

        {/* Big Score Percentage */}
        <div className="p-4 rounded-3xl bg-slate-50 dark:bg-slate-800/60 border border-slate-200/80 dark:border-slate-700/80 space-y-2">
          <div className="text-4xl sm:text-5xl font-black text-indigo-600 dark:text-indigo-400">
            {accuracy}%
          </div>
          <p className="text-xs font-bold text-slate-700 dark:text-slate-300">
            {accuracy >= 80
              ? '🏆 Xuất sắc! Bạn nắm rất vững kiến thức này.'
              : accuracy >= 50
              ? '👍 Khá tốt! Hãy xem lại các câu trả lời sai để rút kinh nghiệm.'
              : '📚 Cần luyện tập thêm để cải thiện phản xạ trắc nghiệm.'}
          </p>
        </div>

        {/* Stats Summary Grid */}
        <div className="grid grid-cols-3 gap-3">
          <div className="p-3 rounded-2xl bg-indigo-50/60 dark:bg-indigo-950/40 border border-indigo-100 dark:border-indigo-900/60">
            <span className="text-[11px] font-bold text-slate-500 dark:text-slate-400 block mb-0.5">
              Đã làm
            </span>
            <span className="text-base font-black text-slate-900 dark:text-white">
              {answeredCount}/{total}
            </span>
          </div>

          <div className="p-3 rounded-2xl bg-emerald-50/60 dark:bg-emerald-950/40 border border-emerald-100 dark:border-emerald-900/60">
            <span className="text-[11px] font-bold text-emerald-600 dark:text-emerald-400 block mb-0.5">
              Số câu đúng
            </span>
            <span className="text-base font-black text-emerald-600 dark:text-emerald-400">
              {correctCount}
            </span>
          </div>

          <div className="p-3 rounded-2xl bg-rose-50/60 dark:bg-rose-950/40 border border-rose-100 dark:border-rose-900/60">
            <span className="text-[11px] font-bold text-rose-600 dark:text-rose-400 block mb-0.5">
              Số câu sai
            </span>
            <span className="text-base font-black text-rose-600 dark:text-rose-400">
              {incorrectCount}
            </span>
          </div>
        </div>

        {/* Actions */}
        <div className="space-y-2 pt-2">
          <button
            type="button"
            onClick={onRestartPractice}
            className="w-full flex items-center justify-center gap-2 py-3 rounded-xl bg-indigo-600 hover:bg-indigo-700 text-white font-bold text-xs sm:text-sm shadow-md shadow-indigo-600/20 transition-all active:scale-95 cursor-pointer"
          >
            <RotateCcw className="w-4 h-4" />
            <span>Luyện tập lại từ đầu</span>
          </button>

          <button
            type="button"
            onClick={onClose}
            className="w-full flex items-center justify-center gap-2 py-2.5 rounded-xl bg-slate-100 dark:bg-slate-800 hover:bg-slate-200 dark:hover:bg-slate-700 text-slate-700 dark:text-slate-200 font-bold text-xs sm:text-sm transition-all cursor-pointer"
          >
            <BookOpen className="w-4 h-4" />
            <span>Xem lại chi tiết từng câu</span>
          </button>

          <button
            type="button"
            onClick={() => navigate('/questions')}
            className="w-full flex items-center justify-center gap-2 py-2 rounded-xl text-slate-500 hover:text-slate-800 dark:hover:text-slate-300 font-semibold text-xs transition-colors cursor-pointer"
          >
            <ArrowLeft className="w-3.5 h-3.5" />
            <span>Quay về Ngân hàng câu hỏi</span>
          </button>
        </div>
      </div>
    </div>
  );
};
