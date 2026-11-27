import { Eye, ShieldCheck } from 'lucide-react';
import { cn } from '@/lib/utils';
import type { MySubmissionDetailResponse } from '../types/session.types';

export function SubmissionReviewNotice({ status, reviewedByLecturer, reviewedAt }: Pick<
  MySubmissionDetailResponse, 'status' | 'reviewedByLecturer' | 'reviewedAt'
>) {
  if (status !== 'REVIEWING' && status !== 'RESOLVED') return null;

  const isResolved = status === 'RESOLVED';
  const Icon = isResolved ? ShieldCheck : Eye;
  return (
    <div className={cn(
      'flex items-start gap-3 rounded-2xl border p-4',
      isResolved ? 'border-success/25 bg-success-soft' : 'border-academic/25 bg-academic-soft',
    )}>
      <Icon aria-hidden="true" className={cn('mt-0.5 size-5 shrink-0', isResolved ? 'text-success' : 'text-academic')} />
      <div className="min-w-0 space-y-1 text-sm">
        <p className="font-semibold">{isResolved ? 'Phiên đã được chốt xét duyệt' : 'Phiên đang được xem xét'}</p>
        {reviewedByLecturer && <p>Giảng viên phụ trách: <strong>{reviewedByLecturer}</strong></p>}
        {reviewedAt && (
          <p className="text-muted-foreground">
            {isResolved ? 'Thời gian chốt phiên: ' : 'Bắt đầu xem xét: '}
            {new Date(reviewedAt).toLocaleString('vi-VN')}
          </p>
        )}
      </div>
    </div>
  );
}
