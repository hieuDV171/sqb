import React, { useEffect, useRef, useState } from 'react';
import { ChevronDown, Loader2, Send, XCircle } from 'lucide-react';
import type { RejectionReasonCode } from '../types/review.types';

interface RejectReasonPopoverProps {
  disabled?: boolean;
  isRejected?: boolean;
  isSubmitting?: boolean;
  onConfirm: (code: RejectionReasonCode, reason?: string) => Promise<void>;
}

const QUICK_REASONS: Array<{ code: RejectionReasonCode; label: string }> = [
  { code: 'DUPLICATE', label: 'Trùng lặp' },
  { code: 'INCORRECT_ANSWER', label: 'Sai hoặc thiếu đáp án đúng' },
  { code: 'AMBIGUOUS_OR_INCOMPLETE', label: 'Đề bài mơ hồ/thiếu dữ kiện' },
  { code: 'OUT_OF_SCOPE', label: 'Ngoài phạm vi môn học' },
  { code: 'LOW_QUALITY', label: 'Chất lượng chưa đạt' },
  { code: 'UNSPECIFIED', label: 'Từ chối không nêu lý do' },
];

export const RejectReasonPopover: React.FC<RejectReasonPopoverProps> = ({
  disabled = false,
  isRejected = false,
  isSubmitting = false,
  onConfirm,
}) => {
  const [isOpen, setIsOpen] = useState(false);
  const [showOther, setShowOther] = useState(false);
  const [otherReason, setOtherReason] = useState('');
  const rootRef = useRef<HTMLDivElement>(null);
  const firstActionRef = useRef<HTMLButtonElement>(null);
  const textareaRef = useRef<HTMLTextAreaElement>(null);

  useEffect(() => {
    if (!isOpen) return;

    const handlePointerDown = (event: MouseEvent) => {
      if (!rootRef.current?.contains(event.target as Node)) setIsOpen(false);
    };
    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') setIsOpen(false);
    };

    document.addEventListener('mousedown', handlePointerDown);
    document.addEventListener('keydown', handleKeyDown);
    window.setTimeout(() => firstActionRef.current?.focus(), 0);
    return () => {
      document.removeEventListener('mousedown', handlePointerDown);
      document.removeEventListener('keydown', handleKeyDown);
    };
  }, [isOpen]);

  useEffect(() => {
    if (showOther) textareaRef.current?.focus();
  }, [showOther]);

  const submit = async (code: RejectionReasonCode, reason?: string) => {
    await onConfirm(code, reason);
    setIsOpen(false);
    setShowOther(false);
    setOtherReason('');
  };

  return (
    <div ref={rootRef} className="relative">
      <button
        type="button"
        aria-haspopup="menu"
        aria-expanded={isOpen}
        onClick={() => setIsOpen((value) => !value)}
        disabled={disabled || isSubmitting}
        className="min-h-10 flex items-center gap-1.5 px-3 rounded-xl text-xs font-semibold text-rose-700 dark:text-rose-300 bg-rose-50 hover:bg-rose-100 dark:bg-rose-950/50 dark:hover:bg-rose-900/60 border border-rose-200 dark:border-rose-800 transition-colors cursor-pointer disabled:opacity-50 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-rose-500"
      >
        {isSubmitting ? <Loader2 className="w-3.5 h-3.5 animate-spin" /> : <XCircle className="w-3.5 h-3.5" />}
        <span>{isRejected ? 'Đổi lý do' : 'Từ chối'}</span>
        <ChevronDown className="w-3.5 h-3.5" />
      </button>

      {isOpen && (
        <div
          role="menu"
          aria-label="Chọn lý do từ chối"
          className="absolute z-30 right-0 bottom-full mb-2 w-72 rounded-2xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-900 p-2 shadow-xl"
        >
          {!showOther ? (
            <>
              <p className="px-2 py-1.5 text-[11px] font-bold uppercase tracking-wide text-slate-500">
                Chọn nhanh lý do
              </p>
              {QUICK_REASONS.map((item, index) => (
                <button
                  key={item.code}
                  ref={index === 0 ? firstActionRef : undefined}
                  role="menuitem"
                  type="button"
                  onClick={() => submit(item.code)}
                  className="w-full min-h-10 rounded-xl px-3 py-2 text-left text-xs font-medium text-slate-700 dark:text-slate-200 hover:bg-rose-50 dark:hover:bg-rose-950/50 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-rose-500"
                >
                  {item.label}
                </button>
              ))}
              <button
                role="menuitem"
                type="button"
                onClick={() => setShowOther(true)}
                className="w-full min-h-10 rounded-xl px-3 py-2 text-left text-xs font-semibold text-rose-700 dark:text-rose-300 hover:bg-rose-50 dark:hover:bg-rose-950/50 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-rose-500"
              >
                Lý do khác…
              </button>
            </>
          ) : (
            <form
              onSubmit={(event) => {
                event.preventDefault();
                if (otherReason.trim()) submit('OTHER', otherReason.trim());
              }}
              className="space-y-3 p-1"
            >
              <label htmlFor="other-rejection-reason" className="block text-xs font-bold text-slate-700 dark:text-slate-200">
                Lý do khác
              </label>
              <textarea
                ref={textareaRef}
                id="other-rejection-reason"
                value={otherReason}
                maxLength={1000}
                rows={3}
                onChange={(event) => setOtherReason(event.target.value)}
                placeholder="Mô tả ngắn gọn để sinh viên có thể cải thiện…"
                className="w-full resize-none rounded-xl border border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-800 p-3 text-xs text-slate-800 dark:text-slate-100 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-rose-500"
              />
              <div className="flex items-center justify-between gap-2">
                <button
                  type="button"
                  onClick={() => setShowOther(false)}
                  className="min-h-10 px-3 rounded-xl text-xs font-semibold text-slate-600 hover:bg-slate-100 dark:text-slate-300 dark:hover:bg-slate-800"
                >
                  Quay lại
                </button>
                <button
                  type="submit"
                  disabled={!otherReason.trim() || isSubmitting}
                  className="min-h-10 inline-flex items-center gap-1.5 px-3 rounded-xl bg-rose-600 text-white text-xs font-semibold hover:bg-rose-700 disabled:opacity-50"
                >
                  <Send className="w-3.5 h-3.5" />
                  Xác nhận
                </button>
              </div>
            </form>
          )}
        </div>
      )}
    </div>
  );
};
