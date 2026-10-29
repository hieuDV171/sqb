import { CheckCircle2, Clock3, XCircle } from 'lucide-react';
import { getRejectionReasonLabel } from '@/lib/rejectionReasons';
import type { MySubmissionQuestionDto } from '../types/session.types';

type QuestionReviewResultProps = Pick<
  MySubmissionQuestionDto,
  'status' | 'rejectionCode' | 'rejectionReason'
>;

export function QuestionReviewResult({
  status,
  rejectionCode,
  rejectionReason,
}: QuestionReviewResultProps) {
  if (status === 'APPROVED') {
    return (
      <span className="inline-flex items-center gap-1 rounded-lg border border-emerald-200 bg-emerald-50 px-2.5 py-1 text-[11px] font-bold text-emerald-700 dark:border-emerald-800 dark:bg-emerald-950/50 dark:text-emerald-300">
        <CheckCircle2 className="h-3.5 w-3.5" /> Đã chấp nhận
      </span>
    );
  }

  if (status === 'REJECTED') {
    return (
      <div className="w-full rounded-2xl border border-rose-200 bg-rose-50/80 p-3 text-xs text-rose-900 dark:border-rose-900/60 dark:bg-rose-950/30 dark:text-rose-200">
        <div className="flex items-center gap-1.5 font-bold">
          <XCircle className="h-4 w-4 shrink-0" />
          <span>Đã từ chối: {getRejectionReasonLabel(rejectionCode)}</span>
        </div>
        {rejectionReason && <p className="mt-1.5 leading-relaxed">{rejectionReason}</p>}
      </div>
    );
  }

  return (
    <span className="inline-flex items-center gap-1 rounded-lg border border-amber-200 bg-amber-50 px-2.5 py-1 text-[11px] font-bold text-amber-700 dark:border-amber-800 dark:bg-amber-950/50 dark:text-amber-300">
      <Clock3 className="h-3.5 w-3.5" /> Chờ kết quả
    </span>
  );
}
