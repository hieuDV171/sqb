import { useState } from 'react';
import { Link } from 'react-router-dom';
import { Check, Copy, Heart, MessageSquare, PlayCircle } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { toast } from '@/stores/useToastStore';
import type { SubmissionSessionSummaryDto } from '@/features/session/types/session.types';

export function UserSessionCard({ session, userId }: { session: SubmissionSessionSummaryDto; userId: number }) {
  const practicePath = `/sessions/${session.sessionId}/practice`;
  const [copied, setCopied] = useState(false);

  async function copyCode(code: string) {
    try {
      await navigator.clipboard.writeText(code);
      setCopied(true);
    } catch {
      toast.error('Không thể sao chép mã phiên.');
    }
  }
  return (
    <article className="flex min-w-0 flex-col gap-4 rounded-2xl border border-border bg-card p-4 sm:p-5">
      <div className="flex flex-wrap items-center gap-2">
        {(session.subjectCode || session.subjectName) && (
          <span className="rounded-full bg-academic-soft px-3 py-1 text-xs font-semibold text-academic">{session.subjectCode || session.subjectName}</span>
        )}
        {session.sessionCode && (
          <Button variant="ghost" className="min-h-11 max-w-full px-2 font-mono text-xs text-muted-foreground"
            aria-label={copied ? 'Đã sao chép mã phiên' : 'Sao chép mã phiên'} onClick={() => void copyCode(session.sessionCode)}>
            <span className="truncate">#{session.sessionCode}</span>
            {copied ? <Check aria-hidden="true" /> : <Copy aria-hidden="true" />}
          </Button>
        )}
      </div>
      <div className="min-w-0 flex-1 space-y-1">
        <h2 className="break-words text-lg font-semibold">{session.title}</h2>
        {session.content && <p className="line-clamp-2 break-words text-sm text-muted-foreground">{session.content}</p>}
      </div>
      <footer className="flex flex-wrap items-center justify-between gap-3 border-t border-border pt-3">
        <p className="flex flex-wrap items-center gap-x-3 gap-y-1 text-sm text-muted-foreground">
          <span>{session.questionCounts ?? 0} câu hỏi</span>
          <span className="inline-flex items-center gap-1"><Heart aria-hidden="true" className="size-3.5" /> {session.reactCount ?? 0}<span className="sr-only"> lượt thích</span></span>
          <span className="inline-flex items-center gap-1"><MessageSquare aria-hidden="true" className="size-3.5" /> {session.commentCount ?? 0}<span className="sr-only"> bình luận</span></span>
          {session.createdAt && <time dateTime={session.createdAt}>{new Date(session.createdAt).toLocaleDateString('vi-VN')}</time>}
        </p>
        <Button variant="outline" className="min-h-11" render={<Link to={practicePath} state={{ fromMemberSessions: userId }} />} nativeButton={false}>
          <PlayCircle aria-hidden="true" /> Luyện tập
        </Button>
      </footer>
    </article>
  );
}
