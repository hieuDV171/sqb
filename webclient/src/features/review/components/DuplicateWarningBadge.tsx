import { useState } from 'react';
import { Dialog } from '@base-ui/react/dialog';
import { useQuery } from '@tanstack/react-query';
import { AlertTriangle, Check, X } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { cn } from '@/lib/utils';
import { reviewService } from '../services/reviewService';
import type { DuplicateDetectionTier, DuplicateWarning } from '../types/review.types';

const tierLabels: Record<DuplicateDetectionTier, string> = {
  RULE_BASED: 'So khớp theo quy tắc',
  TRIGRAM: 'Tương đồng từ ngữ',
  P_HASH: 'So khớp dấu vân tay ảnh',
  TEXT_EMBEDDING: 'Tương đồng ngữ nghĩa',
  IMAGE_EMBEDDING: 'Tương đồng hình ảnh',
};

interface DuplicateWarningBadgeProps {
  sessionId: number;
  questionId: number;
  warnings: DuplicateWarning[];
}

export function DuplicateWarningBadge({ sessionId, questionId, warnings }: DuplicateWarningBadgeProps) {
  const [open, setOpen] = useState(false);
  const matches = useQuery({
    queryKey: ['review-duplicate-matches', sessionId, questionId],
    enabled: open,
    queryFn: async ({ signal }) => (await reviewService.getDuplicateMatches(sessionId, questionId, signal)).data ?? [],
  });
  const byId = new Map(matches.data?.map((match) => [match.questionId, match]));

  return (
    <Dialog.Root open={open} onOpenChange={setOpen}>
      <Dialog.Trigger render={<Button variant="outline" className="min-h-11 border-warning/30 bg-warning-soft text-warning" />}>
        <AlertTriangle aria-hidden="true" /> {warnings.length} đối sánh
      </Dialog.Trigger>
      <Dialog.Portal>
        <Dialog.Backdrop className="fixed inset-0 z-50 bg-black/60" />
        <Dialog.Popup className="fixed inset-x-4 top-1/2 z-50 mx-auto max-h-[calc(100dvh-2rem)] max-w-xl -translate-y-1/2 space-y-4 overflow-y-auto rounded-2xl border border-border bg-card p-5 shadow-xl">
          <div className="flex items-start justify-between gap-2">
            <div>
              <Dialog.Title className="text-lg font-semibold">Đối sánh câu hỏi</Dialog.Title>
              <Dialog.Description className="text-sm text-muted-foreground">Đối chiếu các kết quả bên dưới trước khi quyết định duyệt.</Dialog.Description>
            </div>
            <Dialog.Close render={<Button size="icon" variant="ghost" aria-label="Đóng cảnh báo" />}>
              <X aria-hidden="true" />
            </Dialog.Close>
          </div>
          {matches.isPending && <p role="status" className="text-sm text-muted-foreground">Đang tải phương án và lời giải…</p>}
          {matches.isError && (
            <div className="flex flex-wrap items-center gap-2 text-sm" role="alert">
              <p className="text-destructive">Không thể tải chi tiết câu đối sánh.</p>
              <Button type="button" variant="outline" onClick={() => void matches.refetch()}>Thử lại</Button>
            </div>
          )}
          <ul className="space-y-2">
            {warnings.map((warning, index) => {
              const match = byId.get(warning.similarQuestionId);
              const content = match?.content ?? warning.matchedSnippet ?? warning.matchedSnipet;
              const imageUrls = [...new Set([...(match?.imageUrls ?? []), ...(warning.mediaUrl ? [warning.mediaUrl] : [])])];
              return (
              <li key={`${warning.similarQuestionId}-${warning.tier}-${index}`} className="space-y-2 rounded-xl border border-border bg-surface-subtle p-3">
                <div className="flex flex-wrap justify-between gap-2 text-sm font-semibold">
                  <span>{tierLabels[warning.tier] ?? 'Tương đồng'}</span>
                  <span className="tabular-nums text-warning">{warning.kind === 'EXACT' ? 'Trùng chính xác' : warning.kind === 'ANSWER_CONFLICT' ? 'Mâu thuẫn đáp án'
                    : warning.hammingDistance != null ? `Cách ${warning.hammingDistance} bit`
                      : warning.similarityScore != null ? `Điểm ${warning.similarityScore.toFixed(2)}` : null}</span>
                </div>
                <p className="text-sm text-muted-foreground">{warning.scope === 'SAME_SESSION' ? 'Câu cùng phiên' : 'Câu trong ngân hàng'} #{warning.similarQuestionId}</p>
                {content && <blockquote className="wrap-break-word whitespace-pre-wrap border-s-2 border-border ps-3 text-sm">
                  {content}
                </blockquote>}
                {imageUrls.map((url, imageIndex) => (
                  <img key={url} src={url} alt={`Ảnh đối sánh ${imageIndex + 1}`} loading="lazy" className="max-h-40 max-w-full rounded-lg object-contain" />
                ))}
                {match && (
                  <>
                    <p className="pt-1 text-sm font-semibold">Phương án trả lời</p>
                    {match.options.length > 0 ? (
                      <ul aria-label="Phương án trả lời của câu đối sánh" className="grid gap-2 sm:grid-cols-2">
                        {match.options.map((option) => (
                          <li key={option.key} className={cn('min-w-0 rounded-lg border p-2.5 text-sm',
                            option.isCorrect ? 'border-success/30 bg-success-soft' : 'border-border bg-card')}>
                            <p className="wrap-break-word whitespace-pre-wrap"><strong>{option.key}.</strong> {option.text}</p>
                            {option.mediaUrl && <img src={option.mediaUrl} alt={`Ảnh phương án ${option.key}`}
                              loading="lazy" className="mt-1 max-h-36 max-w-full rounded-lg object-contain" />}
                            {option.isCorrect && <p className="mt-1 flex items-center gap-1 text-xs font-semibold text-success">
                              <Check aria-hidden="true" className="size-4" /> Đáp án đúng
                            </p>}
                          </li>
                        ))}
                      </ul>
                    ) : <p className="text-sm text-muted-foreground">Chưa có phương án.</p>}
                    <div className="rounded-lg border border-border bg-card p-2.5 text-sm">
                      <p className="font-semibold">Lời giải</p>
                      <p className="mt-1 wrap-break-word whitespace-pre-wrap text-muted-foreground">{match.explanation?.trim() || 'Chưa có lời giải.'}</p>
                    </div>
                  </>
                )}
                {!match && matches.isSuccess && <p className="text-sm text-muted-foreground">Chi tiết câu đối sánh hiện không khả dụng.</p>}
              </li>
              );
            })}
          </ul>
        </Dialog.Popup>
      </Dialog.Portal>
    </Dialog.Root>
  );
}
