import { useState } from 'react';
import { Link } from 'react-router-dom';
import { Check, ChevronRight, Copy, Edit3, Heart, MessageSquare, PlayCircle, Trash2 } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { cn } from '@/lib/utils';
import { toast } from '@/stores/useToastStore';
import type { SubmissionSessionSummaryDto, SessionStatus } from '../types/session.types';

interface SubmissionCardProps {
  submission: SubmissionSessionSummaryDto;
  onViewDetail: (sessionId: number) => void;
  onEdit?: (submission: SubmissionSessionSummaryDto) => void;
  onDelete?: (sessionId: number, sessionCode: string) => void;
}

const statuses: Record<SessionStatus, { label: string; className: string }> = {
  PENDING: { label: 'Chờ duyệt', className: 'bg-warning-soft text-warning' },
  REVIEWING: { label: 'Đang xem xét', className: 'bg-academic-soft text-academic' },
  RESOLVED: { label: 'Đã duyệt', className: 'bg-success-soft text-success' },
};

export function SubmissionCard({ submission, onViewDetail, onEdit, onDelete }: SubmissionCardProps) {
  const [copied, setCopied] = useState(false);
  const { sessionId, sessionCode, title, status, subjectName, subjectCode, content, createdAt } = submission;
  const badge = statuses[status];
  const editable = status === 'PENDING';

  async function copyCode() {
    try {
      await navigator.clipboard.writeText(sessionCode);
      setCopied(true);
    } catch {
      toast.error('Không thể sao chép mã phiên.');
    }
  }

  return (
    <article className="flex min-w-0 flex-col gap-4 rounded-2xl border border-border bg-card p-4 sm:p-5">
      <header className="flex flex-wrap items-center gap-2">
        <span className={cn('rounded-full px-3 py-1 text-xs font-semibold', badge?.className)}>{badge?.label}</span>
        {subjectCode && <span className="text-xs font-medium text-muted-foreground">{subjectCode}</span>}
        {sessionCode && (
          <Button variant="ghost" className="min-h-11 max-w-full px-2 font-mono text-xs text-muted-foreground"
            aria-label={copied ? 'Đã sao chép mã phiên' : 'Sao chép mã phiên'} onClick={() => void copyCode()}>
            <span className="truncate">#{sessionCode}</span>
            {copied ? <Check aria-hidden="true" /> : <Copy aria-hidden="true" />}
          </Button>
        )}
      </header>
      <div className="flex-1 space-y-2">
        <h2 className="break-words text-lg font-semibold leading-snug">{title || 'Phiên đề xuất câu hỏi'}</h2>
        {subjectName && <p className="break-words text-sm font-medium text-academic">{subjectName}</p>}
        {content && <p className="line-clamp-2 break-words text-sm text-muted-foreground">{content}</p>}
      </div>
      <footer className="space-y-3 border-t border-border pt-3">
        <p className="flex flex-wrap items-center gap-x-3 gap-y-1 text-sm text-muted-foreground">
          <span>{submission.questionCounts ?? 0} câu hỏi</span>
          <span className="inline-flex items-center gap-1"><Heart aria-hidden="true" className="size-3.5" /> {submission.reactCount ?? 0}<span className="sr-only"> lượt thích</span></span>
          <span className="inline-flex items-center gap-1"><MessageSquare aria-hidden="true" className="size-3.5" /> {submission.commentCount ?? 0}<span className="sr-only"> bình luận</span></span>
          {createdAt && <time dateTime={createdAt}>{new Date(createdAt).toLocaleDateString('vi-VN')}</time>}
        </p>
        <div className="flex flex-wrap gap-2">
          <Button variant="outline" className="min-h-11" onClick={() => onViewDetail(sessionId)}>
            Chi tiết <ChevronRight aria-hidden="true" />
          </Button>
          {editable && onEdit && (
            <Button variant="ghost" size="icon" className="size-11" aria-label="Chỉnh sửa phiên" onClick={() => onEdit(submission)}><Edit3 aria-hidden="true" /></Button>
          )}
          {editable && onDelete && (
            <Button variant="ghost" size="icon" className="size-11 text-destructive" aria-label="Xóa phiên" onClick={() => onDelete(sessionId, sessionCode)}><Trash2 aria-hidden="true" /></Button>
          )}
          {status === 'RESOLVED' && (
            <Button className="min-h-11" render={<Link to={`/sessions/${sessionId}/practice`} />} nativeButton={false}>
              <PlayCircle aria-hidden="true" /> Luyện tập
            </Button>
          )}
        </div>
      </footer>
    </article>
  );
}
