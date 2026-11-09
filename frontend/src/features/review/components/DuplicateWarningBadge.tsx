import { Dialog } from '@base-ui/react/dialog';
import { AlertTriangle, X } from 'lucide-react';
import { Button } from '@/components/ui/button';
import type { DuplicateDetectionTier, DuplicateWarning } from '../types/review.types';

const tierLabels: Record<DuplicateDetectionTier, string> = {
  RULE_BASED: 'So khớp theo quy tắc',
  TRIGRAM: 'Tương đồng từ ngữ',
  P_HASH: 'So khớp dấu vân tay ảnh',
  TEXT_EMBEDDING: 'Tương đồng ngữ nghĩa',
  IMAGE_EMBEDDING: 'Tương đồng hình ảnh',
};

export function DuplicateWarningBadge({ warnings }: { warnings: DuplicateWarning[] }) {
  const highest = Math.max(...warnings.map((warning) => warning.similarityScore));
  return (
    <Dialog.Root>
      <Dialog.Trigger render={<Button variant="outline" className="min-h-11 border-warning/30 bg-warning-soft text-warning" />}>
        <AlertTriangle aria-hidden="true" /> {warnings.length} đối sánh · {(highest * 100).toFixed(0)}%
      </Dialog.Trigger>
      <Dialog.Portal>
        <Dialog.Backdrop className="fixed inset-0 z-50 bg-black/60" />
        <Dialog.Popup className="fixed inset-x-4 top-1/2 z-50 mx-auto max-h-[calc(100dvh-2rem)] max-w-xl -translate-y-1/2 space-y-4 overflow-y-auto rounded-2xl border border-border bg-card p-5 shadow-xl">
          <div className="flex items-start justify-between gap-2">
            <div>
              <Dialog.Title className="text-lg font-semibold">Câu hỏi tương đồng</Dialog.Title>
              <Dialog.Description className="text-sm text-muted-foreground">Đối chiếu các kết quả bên dưới trước khi quyết định duyệt.</Dialog.Description>
            </div>
            <Dialog.Close render={<Button size="icon" variant="ghost" aria-label="Đóng cảnh báo" />}>
              <X aria-hidden="true" />
            </Dialog.Close>
          </div>
          <ul className="space-y-2">
            {warnings.map((warning, index) => (
              <li key={`${warning.similarQuestionId}-${warning.tier}-${index}`} className="space-y-2 rounded-xl border border-border bg-surface-subtle p-3">
                <div className="flex flex-wrap justify-between gap-2 text-sm font-semibold">
                  <span>{tierLabels[warning.tier] ?? 'Tương đồng'}</span>
                  <span className="tabular-nums text-warning">{(warning.similarityScore * 100).toFixed(1)}%</span>
                </div>
                <p className="text-sm text-muted-foreground">Câu trong ngân hàng #{warning.similarQuestionId}</p>
                {warning.matchedSnipet && <blockquote className="wrap-break-word border-s-2 border-border ps-3 text-sm">{warning.matchedSnipet}</blockquote>}
                {warning.mediaUrl && <img src={warning.mediaUrl} alt="Ảnh đối sánh" loading="lazy" className="max-h-32 max-w-full rounded-lg object-contain" />}
              </li>
            ))}
          </ul>
        </Dialog.Popup>
      </Dialog.Portal>
    </Dialog.Root>
  );
}
