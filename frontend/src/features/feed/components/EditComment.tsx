import { useId, useRef, useState } from 'react';
import { useMutation, useQueryClient, type InfiniteData } from '@tanstack/react-query';
import { Button } from '@/components/ui/button';
import { useAuthStore } from '@/stores/useAuthStore';
import type { CommentResponseDto } from '@/types/post.types';
import type { CursorResponse } from '@/types/response.types';
import { commentService } from '../services/commentService';

export function EditComment({ comment }: { comment: CommentResponseDto }) {
  const userId = useAuthStore((state) => state.user?.id);
  const queryClient = useQueryClient();
  const fieldId = useId();
  const editButton = useRef<HTMLButtonElement>(null);
  const [draft, setDraft] = useState<string | null>(null);
  const mutation = useMutation({
    mutationFn: (content: string) => commentService.updateComment(comment.commentId, { content }),
    onSuccess: ({ data }) => {
      const target = [comment.targetType, comment.targetId] as const;
      const update = (item: CommentResponseDto) => item.commentId === comment.commentId
        ? { ...item, content: data.content }
        : item;
      const updateList = (items: CommentResponseDto[]) => items.map((item) => {
        const updated = update(item);
        return item.replies ? { ...updated, replies: item.replies.map(update) } : updated;
      });
      for (const key of ['comments', 'comment-replies']) {
        queryClient.setQueriesData<InfiniteData<CursorResponse<CommentResponseDto>>>({ queryKey: [key, ...target] }, (current) =>
          current && { ...current, pages: current.pages.map((page) => ({ ...page, items: updateList(page.items) })) },
        );
      }
      if (comment.targetType === 'QUESTION') {
        queryClient.setQueryData<CommentResponseDto[]>(['questionComments', comment.targetId], (current) =>
          current && updateList(current),
        );
      }
      close();
    },
  });

  function close() {
    setDraft(null);
    requestAnimationFrame(() => editButton.current?.focus());
  }

  if (userId == null || userId !== comment.author?.userId || comment.deleted) return null;

  if (draft === null) return (
    <Button ref={editButton} type="button" variant="ghost" size="sm" className="min-h-11"
      onClick={() => { mutation.reset(); setDraft(comment.content); }}>
      Sửa
    </Button>
  );

  return (
    <form className="my-2 w-full space-y-2" onSubmit={(event) => {
      event.preventDefault();
      if (!mutation.isPending && draft.trim() && draft.trim() !== comment.content.trim()) mutation.mutate(draft.trim());
    }} onKeyDown={(event) => {
      if (event.key === 'Escape' && !mutation.isPending) { event.stopPropagation(); close(); }
    }}>
      <label htmlFor={fieldId} className="block text-sm font-medium">Sửa nội dung bình luận</label>
      <textarea id={fieldId} autoFocus rows={3} value={draft} onChange={(event) => setDraft(event.target.value)}
        readOnly={mutation.isPending} aria-describedby={mutation.isError ? `${fieldId}-error` : undefined}
        className="w-full rounded-xl border border-input bg-background px-3 py-2 text-sm text-foreground focus-visible:outline-2 focus-visible:outline-ring" />
      {mutation.isError && <p id={`${fieldId}-error`} role="alert" className="text-sm text-destructive">Không lưu được bình luận. Vui lòng thử lại.</p>}
      <div className="flex flex-wrap gap-2">
        <Button type="submit" className="min-h-11" disabled={mutation.isPending || !draft.trim() || draft.trim() === comment.content.trim()}>
          {mutation.isPending ? 'Đang lưu…' : 'Lưu'}
        </Button>
        <Button type="button" variant="ghost" className="min-h-11" disabled={mutation.isPending} onClick={close}>Hủy</Button>
      </div>
    </form>
  );
}
