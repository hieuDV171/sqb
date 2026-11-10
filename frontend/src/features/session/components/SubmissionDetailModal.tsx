import { useState } from 'react';
import { Dialog } from '@base-ui/react/dialog';
import {
  X,
  Clock,
  Eye,
  CheckCircle2,
  Check,
  Copy,
  User,
  Bot,
  Lightbulb,
  Edit3,
  Trash2,
  BookOpen,
  Calendar,
  MessageSquare,
  Heart,
} from 'lucide-react';
import { cn } from '@/lib/utils';
import type {
  MySubmissionDetailResponse,
  SessionStatus,
} from '../types/session.types';
import { QuestionReviewResult } from './QuestionReviewResult';
import { SubmissionReviewNotice } from './SubmissionReviewNotice';
import { ErrorState } from '@/components/ui/error-state';
import { Button } from '@/components/ui/button';

interface SubmissionDetailModalProps {
  detail?: MySubmissionDetailResponse | null;
  isLoading: boolean;
  errorMessage?: string;
  onRetry?: () => void;
  isOpen: boolean;
  onClose: () => void;
  onEdit?: (detail: MySubmissionDetailResponse) => void;
  onDelete?: (sessionId: number, sessionCode: string) => void;
}

export const SubmissionDetailModal: React.FC<SubmissionDetailModalProps> = ({
  detail,
  isLoading,
  errorMessage,
  onRetry,
  isOpen,
  onClose,
  onEdit,
  onDelete,
}) => {
  const [copied, setCopied] = useState(false);
  const [zoomedImage, setZoomedImage] = useState<string | null>(null);

  if (!isOpen) return null;

  const handleCopyCode = () => {
    if (!detail?.sessionCode) return;
    navigator.clipboard.writeText(detail.sessionCode);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const effectiveStatus: SessionStatus = detail?.status ?? 'PENDING';

  const getStatusBadge = (status: SessionStatus) => {
    switch (status) {
      case 'PENDING':
        return {
          label: 'Chờ duyệt',
          icon: Clock,
          className:
            'bg-amber-500/10 text-amber-600 dark:text-amber-400 border-amber-500/20',
        };
      case 'REVIEWING':
        return {
          label: 'Đang xem xét',
          icon: Eye,
          className:
            'bg-blue-500/10 text-blue-600 dark:text-blue-400 border-blue-500/20',
        };
      case 'RESOLVED':
        return {
          label: 'Đã xét duyệt',
          icon: CheckCircle2,
          className:
            'bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 border-emerald-500/20',
        };
      default:
        return {
          label: 'Chờ duyệt',
          icon: Clock,
          className:
            'bg-amber-500/10 text-amber-600 dark:text-amber-400 border-amber-500/20',
        };
    }
  };

  const statusBadge = getStatusBadge(effectiveStatus);
  const StatusIcon = statusBadge.icon;
  const isEditable = effectiveStatus === 'PENDING';

  return (
    <Dialog.Root open={isOpen} onOpenChange={(open) => { if (!open) onClose(); }}>
      <Dialog.Portal>
        <Dialog.Backdrop className="fixed inset-0 z-50 bg-black/60" />
        <Dialog.Popup className="fixed inset-x-3 top-1/2 z-50 mx-auto flex max-h-[calc(100dvh-2rem)] max-w-4xl -translate-y-1/2 flex-col overflow-hidden rounded-2xl border border-border bg-card shadow-xl">
        {/* Modal Header */}
        <div className="p-5 sm:p-6 border-b border-border flex items-start justify-between gap-4">
          <div className="space-y-1.5 flex-1 min-w-0">
            <div className="flex items-center gap-2 flex-wrap">
              <span className="px-2.5 py-1 rounded-xl text-xs font-bold bg-indigo-50 dark:bg-indigo-950/60 text-indigo-700 dark:text-indigo-300 border border-indigo-200/60 dark:border-indigo-800/60">
                {detail?.subjectCode || 'MÔN HỌC'}
              </span>

              <button
                type="button"
                onClick={handleCopyCode}
                className="flex items-center gap-1 px-2.5 py-1 rounded-xl text-xs font-mono font-medium bg-slate-100 dark:bg-slate-800 text-foreground hover:bg-slate-200 dark:hover:bg-slate-700 transition-colors cursor-pointer"
              >
                <span>{detail?.sessionCode}</span>
                {copied ? (
                  <Check className="w-3 h-3 text-emerald-500" />
                ) : (
                  <Copy className="w-3 h-3 text-slate-400" />
                )}
              </button>

              <span
                className={cn(
                  'flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-semibold border',
                  statusBadge.className
                )}
              >
                <StatusIcon className="w-3 h-3" />
                <span>{statusBadge.label}</span>
              </span>
            </div>

            <Dialog.Title className="break-words text-lg font-bold text-foreground sm:text-xl">
              {detail?.title || 'Chi tiết phiên nộp câu hỏi'}
            </Dialog.Title>

            <p className="text-xs text-muted-foreground">
              Môn học: <strong className="text-foreground">{detail?.subjectName}</strong>
            </p>
            {(detail?.classCode || detail?.semesterName) && (
              <p className="text-xs text-muted-foreground">
                Lớp học phần: <strong>{detail?.classCode || '—'}</strong>
                {detail?.semesterName && <> · Học kỳ <strong>{detail.semesterName}</strong></>}
              </p>
            )}
          </div>

          <Dialog.Close render={<Button variant="ghost" size="icon" className="size-11" aria-label="Đóng chi tiết phiên" />}>
            <X aria-hidden="true" />
          </Dialog.Close>
        </div>

        {/* Modal Body */}
        <div className="flex-1 overflow-y-auto p-5 sm:p-6 space-y-6 scrollbar-thin">
          {isLoading ? (
            <div className="flex flex-col items-center justify-center py-16 space-y-3">
              <div className="w-8 h-8 border-3 border-indigo-600 border-t-transparent rounded-full animate-spin" />
              <p className="text-xs text-slate-500">Đang tải thông tin chi tiết phiên...</p>
            </div>
          ) : !detail ? (
            <ErrorState title="Không thể tải chi tiết phiên" message={errorMessage} onRetry={onRetry} />
          ) : (
            <>
              {/* Description / Content */}
              {detail.content && (
                <div className="bg-surface-subtle p-4 rounded-2xl border border-border text-xs sm:text-sm text-foreground leading-relaxed">
                  <span className="font-semibold block text-foreground mb-1">
                    Mô tả / Ghi chú đề xuất:
                  </span>
                  {detail.content}
                </div>
              )}

              <SubmissionReviewNotice status={effectiveStatus} reviewedByLecturer={detail.reviewedByLecturer} reviewedAt={detail.reviewedAt} />

              {/* Questions List */}
              <div className="space-y-4">
                <div className="flex items-center justify-between">
                  <h4 className="text-sm font-bold text-foreground flex items-center gap-2">
                    <BookOpen className="w-4 h-4 text-indigo-600" />
                    <span>Danh sách câu hỏi ({detail.questions?.length || 0})</span>
                  </h4>
                </div>

                <div className="space-y-4">
                  {detail.questions?.map((q, idx) => (
                    <div
                      key={q.questionId || idx}
                      className="bg-white dark:bg-slate-900 border border-border rounded-2xl p-4 sm:p-5 shadow-xs space-y-3"
                    >
                      {/* Question Header */}
                      <div className="flex items-center justify-between gap-2 flex-wrap">
                        <span className="text-xs font-bold px-2.5 py-0.5 rounded-lg bg-indigo-100/70 dark:bg-indigo-950/60 text-indigo-700 dark:text-indigo-300">
                          Câu {idx + 1}
                        </span>

                        <div className="flex items-center gap-2 text-xs">
                          {q.source === 'LLM' ? (
                            <span className="flex items-center gap-1 px-2 py-0.5 rounded-md bg-purple-50 dark:bg-purple-950/40 text-purple-600 dark:text-purple-300 font-medium">
                              <Bot className="w-3 h-3" />
                              <span>AI Generated</span>
                            </span>
                          ) : (
                            <span className="flex items-center gap-1 px-2 py-0.5 rounded-md bg-slate-100 dark:bg-slate-800 text-foreground font-medium">
                              <User className="w-3 h-3" />
                              <span>Tự soạn thảo</span>
                            </span>
                          )}

                          {q.confidenceScore != null && (
                            <span className="text-[11px] text-slate-400 font-mono">
                              Độ tự tin: {q.confidenceScore.toFixed(1)}/4.0
                            </span>
                          )}
                        </div>
                      </div>

                      {/* Question Content */}
                      <p className="text-sm font-medium text-foreground leading-relaxed whitespace-pre-wrap">
                        {q.content}
                      </p>

                      <QuestionReviewResult
                        status={q.status}
                        rejectionCode={q.rejectionCode}
                        rejectionReason={q.rejectionReason}
                      />

                      {/* Question Media Images */}
                      {q.imageUrls && q.imageUrls.length > 0 && (
                        <div className="flex gap-2 flex-wrap pt-1">
                          {q.imageUrls.map((img, i) => (
                            <img
                              key={i}
                              src={img}
                              alt={`Minh họa câu hỏi ${idx + 1}`}
                              onClick={() => setZoomedImage(img)}
                              className="w-24 h-24 object-cover rounded-xl border border-slate-200 dark:border-slate-700 cursor-zoom-in hover:opacity-90 transition-opacity"
                            />
                          ))}
                        </div>
                      )}

                      {/* Options */}
                      <div className="space-y-2 pt-2">
                        {q.options?.map((opt) => (
                          <div
                            key={opt.key}
                            className={cn(
                              'p-3 rounded-xl border text-xs sm:text-sm flex items-start gap-3 transition-colors',
                              opt.isCorrect
                                ? 'bg-emerald-50/70 dark:bg-emerald-950/30 border-emerald-300 dark:border-emerald-800/80 text-emerald-900 dark:text-emerald-200 font-medium'
                                : 'bg-slate-50/70 dark:bg-slate-800/30 border-slate-200 dark:border-slate-700/60 text-foreground'
                            )}
                          >
                            <span
                              className={cn(
                                'w-6 h-6 rounded-lg flex items-center justify-center font-bold text-xs shrink-0',
                                opt.isCorrect
                                  ? 'bg-emerald-600 text-white shadow-xs'
                                  : 'bg-slate-200 dark:bg-slate-700 text-foreground'
                              )}
                            >
                              {opt.key}
                            </span>

                            <div className="flex-1 space-y-1.5 min-w-0">
                              <p className="leading-snug">{opt.text}</p>
                              {opt.mediaUrl && (
                                <img
                                  src={opt.mediaUrl}
                                  alt={`Đáp án ${opt.key}`}
                                  onClick={() => setZoomedImage(opt.mediaUrl!)}
                                  className="w-16 h-16 object-cover rounded-lg border border-slate-200 dark:border-slate-700 cursor-zoom-in mt-1"
                                />
                              )}
                            </div>

                            {opt.isCorrect && (
                              <span className="flex items-center gap-1 text-[11px] font-bold text-emerald-600 dark:text-emerald-400 shrink-0">
                                <Check className="w-3.5 h-3.5" />
                                <span>Đáp án đúng</span>
                              </span>
                            )}
                          </div>
                        ))}
                      </div>

                      {/* Explanation */}
                      {q.explanation && (
                        <div className="p-3 bg-amber-50/50 dark:bg-amber-950/20 border border-amber-200/60 dark:border-amber-900/40 rounded-xl text-xs text-amber-900 dark:text-amber-200 flex items-start gap-2.5">
                          <Lightbulb className="w-4 h-4 text-amber-600 shrink-0 mt-0.5" />
                          <div>
                            <span className="font-bold">Giải thích: </span>
                            <span className="leading-relaxed">{q.explanation}</span>
                          </div>
                        </div>
                      )}
                    </div>
                  ))}
                </div>
              </div>
            </>
          )}
        </div>

        {/* Modal Footer */}
        <div className="p-4 sm:p-5 border-t border-border bg-slate-50 dark:bg-slate-900/60 flex items-center justify-between gap-3">
          <div className="flex items-center gap-4 text-xs text-slate-500">
            <span className="flex items-center gap-1">
              <Calendar className="w-3.5 h-3.5" />
              <span>
                {detail?.createdAt
                  ? new Date(detail.createdAt).toLocaleDateString('vi-VN')
                  : ''}
              </span>
            </span>
            <span className="flex items-center gap-1">
              <Heart className="w-3.5 h-3.5" />
              <span>{detail?.reactCount || 0}</span>
            </span>
            <span className="flex items-center gap-1">
              <MessageSquare className="w-3.5 h-3.5" />
              <span>{detail?.commentCount || 0}</span>
            </span>
          </div>

          <div className="flex items-center gap-2">
            {isEditable && onEdit && detail && (
              <button
                type="button"
                onClick={() => {
                  onEdit(detail);
                }}
                className="flex items-center gap-1.5 px-4 py-2 rounded-xl bg-indigo-50 dark:bg-indigo-950/60 text-indigo-700 dark:text-indigo-300 hover:bg-indigo-100 font-semibold text-xs sm:text-sm transition-colors cursor-pointer"
              >
                <Edit3 className="w-4 h-4" />
                <span>Chỉnh sửa</span>
              </button>
            )}

            {isEditable && onDelete && detail && (
              <button
                type="button"
                onClick={() => {
                  onDelete(detail.sessionId, detail.sessionCode);
                }}
                className="flex items-center gap-1.5 px-4 py-2 rounded-xl bg-rose-50 dark:bg-rose-950/60 text-rose-600 dark:text-rose-400 hover:bg-rose-100 font-semibold text-xs sm:text-sm transition-colors cursor-pointer"
              >
                <Trash2 className="w-4 h-4" />
                <span>Xóa phiên</span>
              </button>
            )}

            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 rounded-xl bg-slate-200 dark:bg-slate-800 text-foreground hover:bg-slate-300 dark:hover:bg-slate-700 font-medium text-xs sm:text-sm transition-colors cursor-pointer"
            >
              Đóng
            </button>
          </div>
        </div>
          <Dialog.Root open={Boolean(zoomedImage)} onOpenChange={(open) => { if (!open) setZoomedImage(null); }}>
            <Dialog.Portal>
              <Dialog.Backdrop className="fixed inset-0 z-60 bg-black/70" />
              <Dialog.Popup className="fixed inset-x-4 top-1/2 z-60 mx-auto max-w-4xl -translate-y-1/2 rounded-2xl border border-border bg-card p-4">
                <div className="mb-3 flex items-center justify-between gap-2">
                  <Dialog.Title className="font-semibold">Hình minh họa</Dialog.Title>
                  <Dialog.Close render={<Button variant="ghost" size="icon" className="size-11" aria-label="Đóng hình" />}><X aria-hidden="true" /></Dialog.Close>
                </div>
                {zoomedImage && <img src={zoomedImage} alt="Hình minh họa trong phiên" className="mx-auto max-h-[72dvh] max-w-full object-contain" />}
              </Dialog.Popup>
            </Dialog.Portal>
          </Dialog.Root>
        </Dialog.Popup>
      </Dialog.Portal>
    </Dialog.Root>
  );
};
