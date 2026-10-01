import React, { useState } from 'react';
import {
  Check,
  X,
  Lightbulb,
  Heart,
  BarChart3,
  Star,
  MessageSquare,
  Bot,
  User,
  ArrowRight,
  ArrowLeft,
  Loader2,
  CheckCircle2,
  XCircle,
  Maximize2,
} from 'lucide-react';
import { cn } from '@/lib/utils';
import { reactService } from '@/features/feed/services/reactService';
import type { PracticeQuestionDto } from '../types/practice.types';

interface PracticeQuestionCardProps {
  question: PracticeQuestionDto;
  questionIndex: number;
  totalQuestions: number;
  selectedOptions: string[];
  onToggleOption: (key: string) => void;
  onSubmitAnswer: () => void;
  isSubmittingAnswer: boolean;
  onPrevQuestion: () => void;
  onNextQuestion: () => void;
  hasPrev: boolean;
  hasNext: boolean;
  onOpenStats: (questionId: number, code?: string) => void;
  onOpenRate: (questionId: number, code?: string) => void;
  onOpenDiscussion: (questionId: number, code?: string) => void;
  localAnswerResult?: { isCorrect: boolean; selected: string[] };
}

export const PracticeQuestionCard: React.FC<PracticeQuestionCardProps> = ({
  question,
  questionIndex,
  totalQuestions,
  selectedOptions,
  onToggleOption,
  onSubmitAnswer,
  isSubmittingAnswer,
  onPrevQuestion,
  onNextQuestion,
  hasPrev,
  hasNext,
  onOpenStats,
  onOpenRate,
  onOpenDiscussion,
  localAnswerResult,
}) => {
  const [lightboxImg, setLightboxImg] = useState<string | null>(null);
  const [isLiked, setIsLiked] = useState<boolean>(false);
  const [likesCount, setLikesCount] = useState<number>(question.reactCount || 0);

  const isAnswered = question.myInteraction.answered || !!localAnswerResult;
  const isCorrect = localAnswerResult
    ? localAnswerResult.isCorrect
    : undefined;

  const correctAnswerStr = question.hiddenFields?.correctAnswer || '';
  const correctKeys = correctAnswerStr
    ? correctAnswerStr.split(',').map((k) => k.trim())
    : [];

  const handleToggleLike = async () => {
    try {
      setIsLiked(!isLiked);
      setLikesCount((prev) => (isLiked ? Math.max(0, prev - 1) : prev + 1));
      await reactService.toggleReact({
        targetType: 'QUESTION',
        targetId: question.questionId,
        reactionType: 'LIKE',
      });
    } catch (err) {
      // rollback
      setIsLiked(isLiked);
      setLikesCount(question.reactCount || 0);
    }
  };

  return (
    <div className="rounded-3xl bg-white dark:bg-slate-900 border border-slate-200/80 dark:border-slate-800 shadow-md overflow-hidden transition-all space-y-6 p-5 sm:p-8">
      {/* Card Header */}
      <div className="flex flex-wrap items-center justify-between gap-3 border-b border-slate-100 dark:border-slate-800 pb-4">
        {/* Left: Question Number & Source Badges */}
        <div className="flex items-center gap-2">
          <span className="px-3 py-1 rounded-xl bg-indigo-600 text-white font-black text-xs sm:text-sm tracking-tight shadow-xs">
            Câu {questionIndex + 1} / {totalQuestions}
          </span>

          {question.source === 'LLM' ? (
            <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-xl text-[11px] font-bold bg-purple-50 dark:bg-purple-950/60 text-purple-600 dark:text-purple-400 border border-purple-200/60 dark:border-purple-800/60">
              <Bot className="w-3.5 h-3.5" /> AI Sinh
            </span>
          ) : (
            <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-xl text-[11px] font-bold bg-blue-50 dark:bg-blue-950/60 text-blue-600 dark:text-blue-400 border border-blue-200/60 dark:border-blue-800/60">
              <User className="w-3.5 h-3.5" /> Tự biên soạn
            </span>
          )}

          {question.questionCode && (
            <span className="text-slate-400 font-mono text-[11px] hidden sm:inline">
              #{question.questionCode}
            </span>
          )}
        </div>

        {/* Right: Social & Interaction Actions */}
        <div className="flex items-center gap-1 sm:gap-1.5">
          {/* Reaction Button */}
          <button
            type="button"
            onClick={handleToggleLike}
            className={cn(
              'p-2 rounded-xl transition-all cursor-pointer flex items-center gap-1 text-xs font-semibold',
              isLiked
                ? 'bg-rose-50 dark:bg-rose-950/60 text-rose-600 dark:text-rose-400'
                : 'text-slate-500 hover:text-rose-500 hover:bg-slate-100 dark:hover:bg-slate-800'
            )}
            title="Thích câu hỏi này"
          >
            <Heart className={cn('w-4 h-4', isLiked && 'fill-rose-500')} />
            {likesCount > 0 && <span>{likesCount}</span>}
          </button>

          {/* Stats Button */}
          <button
            type="button"
            onClick={() => onOpenStats(question.questionId, question.questionCode)}
            className="p-2 rounded-xl text-slate-500 hover:text-indigo-600 hover:bg-indigo-50 dark:hover:bg-indigo-950/50 transition-all cursor-pointer"
            title="Xem thống kê tỷ lệ đúng và bẫy trắc nghiệm"
          >
            <BarChart3 className="w-4 h-4" />
          </button>

          {/* Rating Button */}
          <button
            type="button"
            onClick={() => onOpenRate(question.questionId, question.questionCode)}
            className={cn(
              'p-2 rounded-xl transition-all cursor-pointer flex items-center gap-1 text-xs font-semibold',
              question.myInteraction.rated
                ? 'text-amber-500 bg-amber-50 dark:bg-amber-950/60'
                : 'text-slate-500 hover:text-amber-500 hover:bg-amber-50 dark:hover:bg-amber-950/50'
            )}
            title="Đánh giá chất lượng hoặc Báo lỗi câu hỏi"
          >
            <Star
              className={cn(
                'w-4 h-4',
                question.myInteraction.rated && 'fill-amber-500 text-amber-500'
              )}
            />
            {question.ratingCount > 0 && <span>{question.ratingCount}</span>}
          </button>

          {/* Discussion Button */}
          <button
            type="button"
            onClick={() => onOpenDiscussion(question.questionId, question.questionCode)}
            className="p-2 rounded-xl text-slate-500 hover:text-indigo-600 hover:bg-indigo-50 dark:hover:bg-indigo-950/50 transition-all cursor-pointer flex items-center gap-1 text-xs font-semibold"
            title="Thảo luận và hỏi đáp về câu hỏi"
          >
            <MessageSquare className="w-4 h-4" />
            {question.commentCount > 0 && <span>{question.commentCount}</span>}
          </button>
        </div>
      </div>

      {/* Question Content */}
      <div className="space-y-4">
        <div className="text-slate-900 dark:text-slate-100 text-sm sm:text-base leading-relaxed font-medium whitespace-pre-line">
          {question.content}
        </div>

        {/* Question Image Attachments */}
        {question.imageUrls && question.imageUrls.length > 0 && (
          <div className="flex flex-wrap gap-3 pt-1">
            {question.imageUrls.map((url, i) => (
              <div
                key={i}
                onClick={() => setLightboxImg(url)}
                className="group relative rounded-2xl overflow-hidden border border-slate-200 dark:border-slate-800 bg-slate-50 dark:bg-slate-800/40 h-32 sm:h-44 w-auto cursor-pointer shadow-xs hover:shadow-md transition-all"
              >
                <img
                  src={url}
                  alt={`Question Media ${i + 1}`}
                  className="h-full w-auto object-contain transition-transform duration-300 group-hover:scale-105"
                />
                <div className="absolute inset-0 bg-slate-950/20 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center text-white">
                  <Maximize2 className="w-5 h-5 drop-shadow-md" />
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Options List */}
      <div className="space-y-3 pt-2">
        {question.options.map((opt) => {
          const isSelected = selectedOptions.includes(opt.key);
          const isCorrectOption = correctKeys.includes(opt.key);

          let optionStyle = 'bg-slate-50/80 dark:bg-slate-800/50 border-slate-200 dark:border-slate-700/80 text-slate-700 dark:text-slate-300 hover:border-indigo-400 hover:bg-slate-100/80';
          let badgeStyle = 'bg-slate-200 dark:bg-slate-700 text-slate-700 dark:text-slate-200';

          if (!isAnswered) {
            if (isSelected) {
              optionStyle = 'bg-indigo-50/90 dark:bg-indigo-950/60 border-indigo-500 text-indigo-950 dark:text-indigo-100 ring-1 ring-indigo-500 shadow-xs';
              badgeStyle = 'bg-indigo-600 text-white';
            }
          } else {
            // Already answered: reveal correctness
            if (isCorrectOption) {
              optionStyle = 'bg-emerald-50 dark:bg-emerald-950/60 border-emerald-500 text-emerald-950 dark:text-emerald-100 ring-2 ring-emerald-500/50 shadow-xs';
              badgeStyle = 'bg-emerald-600 text-white';
            } else if (isSelected && !isCorrectOption) {
              optionStyle = 'bg-rose-50 dark:bg-rose-950/60 border-rose-500 text-rose-950 dark:text-rose-100 ring-1 ring-rose-500 shadow-xs line-through opacity-85';
              badgeStyle = 'bg-rose-600 text-white';
            } else {
              optionStyle = 'bg-slate-50/50 dark:bg-slate-800/30 border-slate-200/60 dark:border-slate-800 text-slate-400 dark:text-slate-500';
              badgeStyle = 'bg-slate-200/70 dark:bg-slate-800 text-slate-400';
            }
          }

          return (
            <div
              key={opt.key}
              onClick={() => {
                if (!isAnswered) onToggleOption(opt.key);
              }}
              className={cn(
                'group flex items-start gap-3 p-3.5 sm:p-4 rounded-2xl border transition-all duration-200',
                optionStyle,
                !isAnswered ? 'cursor-pointer active:scale-[0.99]' : 'cursor-default'
              )}
            >
              {/* Option Key Badge (A, B, C, D) */}
              <div
                className={cn(
                  'w-7 h-7 sm:w-8 sm:h-8 rounded-xl flex items-center justify-center font-black text-xs sm:text-sm shrink-0 transition-all',
                  badgeStyle
                )}
              >
                {opt.key}
              </div>

              {/* Option Text & Media */}
              <div className="flex-1 min-w-0 pt-0.5 space-y-2">
                <div className="text-xs sm:text-sm leading-relaxed">{opt.text}</div>

                {opt.mediaUrl && (
                  <div
                    onClick={(e) => {
                      e.stopPropagation();
                      setLightboxImg(opt.mediaUrl!);
                    }}
                    className="inline-block rounded-xl overflow-hidden border border-slate-200 dark:border-slate-700 h-16 sm:h-20 w-auto cursor-pointer hover:opacity-90 shadow-2xs"
                  >
                    <img
                      src={opt.mediaUrl}
                      alt={`Option ${opt.key}`}
                      className="h-full w-auto object-contain"
                    />
                  </div>
                )}
              </div>

              {/* Status Icons */}
              {isAnswered && (
                <div className="shrink-0 self-center pl-2">
                  {isCorrectOption ? (
                    <div className="flex items-center gap-1 text-emerald-600 dark:text-emerald-400 font-bold text-xs">
                      <Check className="w-5 h-5 text-emerald-600" />
                      <span className="hidden sm:inline">Đáp án đúng</span>
                    </div>
                  ) : isSelected ? (
                    <div className="flex items-center gap-1 text-rose-600 dark:text-rose-400 font-bold text-xs">
                      <X className="w-5 h-5 text-rose-600" />
                      <span className="hidden sm:inline">Lựa chọn sai</span>
                    </div>
                  ) : null}
                </div>
              )}
            </div>
          );
        })}
      </div>

      {/* Answer Button or Result Alert */}
      {!isAnswered ? (
        <div className="pt-2 flex justify-end">
          <button
            type="button"
            onClick={onSubmitAnswer}
            disabled={selectedOptions.length === 0 || isSubmittingAnswer}
            className="flex items-center gap-2 px-6 py-3 rounded-2xl bg-indigo-600 hover:bg-indigo-700 disabled:opacity-40 disabled:cursor-not-allowed text-white text-xs sm:text-sm font-bold shadow-md shadow-indigo-600/25 active:scale-95 transition-all cursor-pointer"
          >
            {isSubmittingAnswer ? (
              <>
                <Loader2 className="w-4 h-4 animate-spin" />
                <span>Đang kiểm tra...</span>
              </>
            ) : (
              <>
                <span>Kiểm tra đáp án</span>
                <Check className="w-4 h-4" />
              </>
            )}
          </button>
        </div>
      ) : (
        /* Result Banner & Explanation Card */
        <div className="space-y-4 pt-2 animate-in fade-in slide-in-from-bottom-2 duration-300">
          {/* Result Alert Box */}
          <div
            className={cn(
              'p-4 rounded-2xl border flex items-center gap-3',
              isCorrect === false
                ? 'bg-rose-50 dark:bg-rose-950/30 border-rose-200 dark:border-rose-900/60 text-rose-900 dark:text-rose-200'
                : 'bg-emerald-50 dark:bg-emerald-950/30 border-emerald-200 dark:border-emerald-900/60 text-emerald-900 dark:text-emerald-200'
            )}
          >
            {isCorrect === false ? (
              <XCircle className="w-6 h-6 text-rose-600 dark:text-rose-400 shrink-0" />
            ) : (
              <CheckCircle2 className="w-6 h-6 text-emerald-600 dark:text-emerald-400 shrink-0" />
            )}
            <div className="text-xs sm:text-sm">
              <span className="font-black block text-sm">
                {isCorrect === false
                  ? 'Chưa chính xác!'
                  : 'Chính xác! Làm rất tốt 🎉'}
              </span>
              <span>
                Đáp án chuẩn của câu hỏi này là: <strong>{correctAnswerStr}</strong>
              </span>
            </div>
          </div>

          {/* Explanation Card */}
          {question.hiddenFields?.explanation && (
            <div className="p-4 sm:p-5 rounded-2xl bg-blue-50/70 dark:bg-blue-950/30 border border-blue-200/80 dark:border-blue-900/60 space-y-2">
              <div className="flex items-center gap-2 text-xs font-bold text-blue-900 dark:text-blue-300">
                <Lightbulb className="w-4 h-4 text-amber-500 fill-amber-500" />
                <span>Giải thích chi tiết:</span>
              </div>
              <p className="text-xs sm:text-sm text-slate-700 dark:text-slate-300 leading-relaxed whitespace-pre-line">
                {question.hiddenFields.explanation}
              </p>
            </div>
          )}
        </div>
      )}

      {/* Card Navigation Footer */}
      <div className="flex items-center justify-between pt-6 border-t border-slate-100 dark:border-slate-800">
        <button
          type="button"
          onClick={onPrevQuestion}
          disabled={!hasPrev}
          className="flex items-center gap-1.5 px-4 py-2 rounded-xl text-xs sm:text-sm font-bold text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800 disabled:opacity-30 disabled:pointer-events-none transition-all cursor-pointer"
        >
          <ArrowLeft className="w-4 h-4" />
          <span>Câu trước</span>
        </button>

        <button
          type="button"
          onClick={onNextQuestion}
          disabled={!hasNext}
          className="flex items-center gap-1.5 px-5 py-2.5 rounded-xl text-xs sm:text-sm font-bold bg-slate-900 dark:bg-white text-white dark:text-slate-900 hover:bg-slate-800 dark:hover:bg-slate-100 disabled:opacity-30 disabled:pointer-events-none transition-all shadow-xs cursor-pointer active:scale-95"
        >
          <span>Câu tiếp theo</span>
          <ArrowRight className="w-4 h-4" />
        </button>
      </div>

      {/* Lightbox Zoom Modal */}
      {lightboxImg && (
        <div
          onClick={() => setLightboxImg(null)}
          className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/80 backdrop-blur-sm cursor-zoom-out animate-in fade-in"
        >
          <img
            src={lightboxImg}
            alt="Enlarged Visual"
            className="max-h-[90vh] max-w-[90vw] object-contain rounded-2xl shadow-2xl"
          />
        </div>
      )}
    </div>
  );
};
