import React, { useState } from 'react';
import type { SessionQuestionReviewDto, DuplicateWarning } from '../types/review.types';
import { DuplicateWarningBadge } from './DuplicateWarningBadge';
import {
  CheckCircle2,
  XCircle,
  Edit3,
  Bot,
  UserCheck,
  Check,
  ZoomIn,
  X,
  HelpCircle,
} from 'lucide-react';
import { cn } from '@/lib/utils';

interface ReviewQuestionCardProps {
  question: SessionQuestionReviewDto;
  questionNumber: number;
  duplicateWarnings: DuplicateWarning[];
  onApprove: (questionId: number) => void;
  onReject: (question: SessionQuestionReviewDto) => void;
  onEdit: (question: SessionQuestionReviewDto) => void;
  disabled?: boolean;
}

export const ReviewQuestionCard: React.FC<ReviewQuestionCardProps> = ({
  question,
  questionNumber,
  duplicateWarnings,
  onApprove,
  onReject,
  onEdit,
  disabled = false,
}) => {
  const [lightboxImg, setLightboxImg] = useState<string | null>(null);

  const getStatusBadge = () => {
    switch (question.status) {
      case 'APPROVED':
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-xl text-xs font-bold bg-emerald-100 dark:bg-emerald-950/60 text-emerald-700 dark:text-emerald-300 border border-emerald-300 dark:border-emerald-800">
            <CheckCircle2 className="w-3.5 h-3.5" />
            <span>Đã phê duyệt</span>
          </span>
        );
      case 'REJECTED':
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-xl text-xs font-bold bg-rose-100 dark:bg-rose-950/60 text-rose-700 dark:text-rose-300 border border-rose-300 dark:border-rose-800">
            <XCircle className="w-3.5 h-3.5" />
            <span>Đã từ chối</span>
          </span>
        );
      case 'EDITING':
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-xl text-xs font-bold bg-blue-100 dark:bg-blue-950/60 text-blue-700 dark:text-blue-300 border border-blue-300 dark:border-blue-800">
            <Edit3 className="w-3.5 h-3.5" />
            <span>Đang biên tập</span>
          </span>
        );
      default:
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-xl text-xs font-bold bg-amber-100 dark:bg-amber-950/60 text-amber-700 dark:text-amber-300 border border-amber-300 dark:border-amber-800">
            <HelpCircle className="w-3.5 h-3.5" />
            <span>Chờ duyệt</span>
          </span>
        );
    }
  };

  return (
    <>
      <div className="bg-white dark:bg-slate-900 rounded-3xl border border-slate-200/90 dark:border-slate-800 p-5 md:p-6 shadow-xs space-y-4 transition-all">
        {/* Top Header */}
        <div className="flex items-center justify-between flex-wrap gap-2 pb-3 border-b border-slate-100 dark:border-slate-800/80">
          <div className="flex items-center gap-2.5">
            <span className="px-3 py-1 rounded-xl bg-slate-900 dark:bg-white text-white dark:text-slate-900 font-extrabold text-xs">
              Câu {questionNumber}
            </span>
            {getStatusBadge()}
          </div>

          {/* Duplicate Warnings */}
          {duplicateWarnings.length > 0 && (
            <DuplicateWarningBadge warnings={duplicateWarnings} />
          )}
        </div>

        {/* Question Text */}
        <div className="text-sm md:text-base font-medium text-slate-800 dark:text-slate-100 leading-relaxed whitespace-pre-line">
          {question.content}
        </div>

        {/* Question Images */}
        {question.imageUrls && question.imageUrls.length > 0 && (
          <div className="flex flex-wrap gap-3 pt-1">
            {question.imageUrls.map((url, i) => (
              <div
                key={i}
                onClick={() => setLightboxImg(url)}
                className="relative rounded-2xl overflow-hidden border border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-800 h-28 w-auto min-w-[110px] group cursor-pointer shadow-2xs"
              >
                <img
                  src={url}
                  alt={`Question media ${i + 1}`}
                  className="h-full w-auto object-contain transition-transform duration-200 group-hover:scale-105"
                />
                <div className="absolute inset-0 bg-black/30 opacity-0 group-hover:opacity-100 flex items-center justify-center transition-opacity">
                  <ZoomIn className="w-5 h-5 text-white" />
                </div>
              </div>
            ))}
          </div>
        )}

        {/* Options List */}
        <div className="space-y-2 pt-2">
          <div className="text-xs font-bold text-slate-500 dark:text-slate-400 uppercase tracking-wider">
            Các phương án lựa chọn:
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-2.5">
            {question.options.map((opt) => (
              <div
                key={opt.key}
                className={cn(
                  'flex items-start gap-2.5 p-3 rounded-2xl border text-xs md:text-sm transition-all',
                  opt.isCorrect
                    ? 'bg-emerald-50/80 dark:bg-emerald-950/40 border-emerald-300 dark:border-emerald-800 text-emerald-950 dark:text-emerald-100 font-semibold shadow-2xs'
                    : 'bg-slate-50/50 dark:bg-slate-800/40 border-slate-200/80 dark:border-slate-800 text-slate-700 dark:text-slate-300'
                )}
              >
                <span
                  className={cn(
                    'w-6 h-6 rounded-lg flex items-center justify-center font-bold text-xs shrink-0 mt-0.5',
                    opt.isCorrect
                      ? 'bg-emerald-600 text-white'
                      : 'bg-slate-200 dark:bg-slate-700 text-slate-700 dark:text-slate-200'
                  )}
                >
                  {opt.key}
                </span>

                <div className="flex-1 min-w-0">
                  <div className="leading-snug">{opt.text}</div>

                  {opt.mediaUrl && (
                    <div
                      onClick={() => setLightboxImg(opt.mediaUrl!)}
                      className="mt-2 inline-block rounded-xl overflow-hidden border border-slate-200 dark:border-slate-700 h-16 w-auto cursor-pointer"
                    >
                      <img
                        src={opt.mediaUrl}
                        alt={`Option ${opt.key}`}
                        className="h-full w-auto object-contain"
                      />
                    </div>
                  )}
                </div>

                {opt.isCorrect && (
                  <Check className="w-4 h-4 text-emerald-600 dark:text-emerald-400 shrink-0 ml-1" />
                )}
              </div>
            ))}
          </div>
        </div>

        {/* Explanation */}
        {question.explanation && (
          <div className="p-3.5 rounded-2xl bg-blue-50/70 dark:bg-blue-950/30 border border-blue-200/80 dark:border-blue-900/60 text-xs space-y-1">
            <span className="font-bold text-blue-900 dark:text-blue-300 block">
              💡 Giải thích đáp án:
            </span>
            <p className="text-slate-700 dark:text-slate-300 leading-relaxed">
              {question.explanation}
            </p>
          </div>
        )}

        {/* Edit Log Audit Info */}
        {question.editLog && (
          <div className="flex items-center gap-2 p-2.5 rounded-xl bg-purple-50/80 dark:bg-purple-950/40 border border-purple-200/70 dark:border-purple-800 text-[11px] text-purple-900 dark:text-purple-200">
            {question.editLog.actorType === 'SYSTEM' ? (
              <Bot className="w-3.5 h-3.5 text-purple-600 dark:text-purple-400 shrink-0" />
            ) : (
              <UserCheck className="w-3.5 h-3.5 text-purple-600 dark:text-purple-400 shrink-0" />
            )}
            <span>
              Đã qua kiểm duyệt/sửa đổi bởi: <strong>{question.editLog.actorName}</strong> ({question.editLog.actorType})
            </span>
          </div>
        )}

        {/* Bottom Actions for Lecturer */}
        <div className="flex items-center justify-between pt-3 border-t border-slate-100 dark:border-slate-800/80 flex-wrap gap-2">
          <div className="text-xs text-slate-500 dark:text-slate-400">
            Mã câu hỏi: <span className="font-mono font-semibold">#{question.questionId}</span>
          </div>

          <div className="flex items-center gap-2 flex-wrap">
            {/* Edit Button */}
            <button
              type="button"
              onClick={() => onEdit(question)}
              disabled={disabled}
              className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl text-xs font-semibold text-blue-700 dark:text-blue-300 bg-blue-50 hover:bg-blue-100 dark:bg-blue-950/50 dark:hover:bg-blue-900/60 border border-blue-200 dark:border-blue-800 transition-colors cursor-pointer"
            >
              <Edit3 className="w-3.5 h-3.5" />
              <span>Biên tập</span>
            </button>

            {/* Reject Button */}
            <button
              type="button"
              onClick={() => onReject(question)}
              disabled={disabled || question.status === 'REJECTED'}
              className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl text-xs font-semibold text-rose-700 dark:text-rose-300 bg-rose-50 hover:bg-rose-100 dark:bg-rose-950/50 dark:hover:bg-rose-900/60 border border-rose-200 dark:border-rose-800 transition-colors cursor-pointer disabled:opacity-50"
            >
              <XCircle className="w-3.5 h-3.5" />
              <span>Từ chối</span>
            </button>

            {/* Approve Button */}
            <button
              type="button"
              onClick={() => onApprove(question.questionId)}
              disabled={disabled || question.status === 'APPROVED'}
              className="flex items-center gap-1.5 px-4 py-1.5 rounded-xl text-xs font-semibold text-white bg-emerald-600 hover:bg-emerald-700 shadow-md shadow-emerald-500/20 transition-all cursor-pointer disabled:opacity-50"
            >
              <CheckCircle2 className="w-3.5 h-3.5" />
              <span>{question.status === 'APPROVED' ? 'Đã duyệt' : 'Phê duyệt'}</span>
            </button>
          </div>
        </div>
      </div>

      {/* Lightbox Preview */}
      {lightboxImg && (
        <div
          onClick={() => setLightboxImg(null)}
          className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-xs cursor-pointer animate-in fade-in duration-200"
        >
          <div className="relative max-w-4xl max-h-[90vh]">
            <img
              src={lightboxImg}
              alt="Zoomed preview"
              className="max-h-[85vh] max-w-full rounded-2xl object-contain shadow-2xl"
            />
            <button
              type="button"
              onClick={() => setLightboxImg(null)}
              className="absolute top-3 right-3 p-2 bg-black/60 hover:bg-black text-white rounded-full transition-colors cursor-pointer"
            >
              <X className="w-5 h-5" />
            </button>
          </div>
        </div>
      )}
    </>
  );
};
