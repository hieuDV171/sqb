import { useState } from 'react';
import { Link } from 'react-router-dom';
import { Check, Copy, MessageSquare, PlayCircle } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { toast } from '@/stores/useToastStore';
import type { SubmissionSessionSummaryDto } from '@/features/session/types/session.types';
import { SessionReactionButton } from './SessionReactionButton';

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
        <div className="flex flex-wrap items-center gap-x-3 gap-y-1 text-sm text-muted-foreground">
          <span>{session.questionCounts ?? 0} câu hỏi</span>
          <SessionReactionButton sessionId={session.sessionId} reactCount={session.reactCount} myReaction={session.myReaction} />
          <Link
            to={`${practicePath}?discussion=session`}
            state={{ fromMemberSessions: userId }}
            className="inline-flex min-h-11 items-center gap-1.5 rounded-lg px-2 font-medium text-academic hover:bg-academic-soft focus-visible:outline-2 focus-visible:outline-ring"
            aria-label={`Thảo luận bộ ${session.title}, ${session.commentCount ?? 0} bình luận`}
          >
            <MessageSquare aria-hidden="true" className="size-3.5" /> {session.commentCount ?? 0} bình luận
          </Link>
          {session.createdAt && <time dateTime={session.createdAt}>{new Date(session.createdAt).toLocaleDateString('vi-VN')}</time>}
        </div>
        <Button variant="outline" className="min-h-11" render={<Link to={practicePath} state={{ fromMemberSessions: userId }} />} nativeButton={false}>
          <PlayCircle aria-hidden="true" /> Luyện tập
        </Button>
      </footer>
    </article>
  );
}
