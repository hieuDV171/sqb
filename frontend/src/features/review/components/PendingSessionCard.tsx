import { Link } from 'react-router-dom';
import { ArrowRight, Clock3, Inbox } from 'lucide-react';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import type { ReviewableSessionItemDto } from '../types/review.types';

export function PendingSessionCard({ session, queueSearch }: { session: ReviewableSessionItemDto; queueSearch: string }) {
  const reviewing = session.status === 'REVIEWING';
  const StatusIcon = reviewing ? Clock3 : Inbox;
  const date = new Date(session.createdAt);
  const title = session.title || 'Đề xuất câu hỏi';
  return (
    <article className="flex flex-col gap-3 rounded-2xl border border-border bg-card p-4 sm:p-5">
      <div className="flex flex-wrap items-center gap-2">
        <Badge variant="outline" className={reviewing ? 'bg-academic-soft text-academic' : 'bg-warning-soft text-warning'}>
          <StatusIcon aria-hidden="true" /> {reviewing ? 'Đang duyệt' : 'Chờ duyệt'}
        </Badge>
        <span className="font-mono text-xs text-muted-foreground">#{session.sessionId}</span>
        {[session.subjectCode, session.classCode, session.semesterName].filter(Boolean).length > 0 && (
          <span className="text-sm text-muted-foreground">
            {[session.subjectCode, session.classCode && `Lớp ${session.classCode}`, session.semesterName].filter(Boolean).join(' · ')}
          </span>
        )}
      </div>
      <div className="space-y-1">
        <h2 className="wrap-break-word text-lg font-semibold">{title}</h2>
        {session.topic && <p className="text-sm font-medium text-academic">{session.topic}</p>}
        {session.content && <p className="line-clamp-2 wrap-break-word text-sm text-muted-foreground">{session.content}</p>}
      </div>
      <footer className="flex flex-wrap items-center justify-between gap-3 border-t border-border pt-3">
        <p className="flex flex-wrap items-center gap-x-3 gap-y-1 text-sm text-muted-foreground">
          <span>{session.author?.fullName || 'Học viên ẩn danh'}</span>
          {!Number.isNaN(date.valueOf()) && <time dateTime={session.createdAt}>{date.toLocaleString('vi-VN')}</time>}
        </p>
        <Button className="min-h-11" nativeButton={false}
          render={<Link to={`/lecturer/sessions/${session.sessionId}/review`} state={{ queueSearch }} />}>
          {reviewing ? 'Tiếp tục duyệt' : 'Mở phiên duyệt'} <ArrowRight aria-hidden="true" />
        </Button>
      </footer>
    </article>
  );
}
