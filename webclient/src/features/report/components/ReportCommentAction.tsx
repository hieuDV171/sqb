import { useState } from 'react';
import { Flag } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { useAuthStore } from '@/stores/useAuthStore';
import type { CommentResponseDto } from '@/types/post.types';
import { ReportModal } from './ReportModal';

export function ReportCommentAction({ comment }: { comment: CommentResponseDto }) {
  const userId = useAuthStore((state) => state.user?.id);
  const [open, setOpen] = useState(false);
  if (userId == null || comment.deleted || comment.author?.userId === userId) return null;

  return (
    <>
      <Button type="button" variant="ghost" size="sm" className="min-h-11 gap-1 text-muted-foreground"
        onClick={() => setOpen(true)} aria-label="Báo cáo bình luận">
        <Flag className="size-3.5" aria-hidden="true" /> Báo cáo
      </Button>
      {open && <ReportModal isOpen onClose={() => setOpen(false)} targetType="COMMENT" targetId={comment.commentId} />}
    </>
  );
}
