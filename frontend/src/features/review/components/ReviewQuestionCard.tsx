import { useState } from 'react';
import { Dialog } from '@base-ui/react/dialog';
import { Check, CircleCheck, Clock3, Pencil, X, XCircle, ZoomIn } from 'lucide-react';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { cn } from '@/lib/utils';
import { getRejectionReasonLabel } from '@/lib/rejectionReasons';
import { DuplicateWarningBadge } from './DuplicateWarningBadge';
import { RejectReasonPopover } from './RejectReasonPopover';
import type { SessionQuestionReviewDto, DuplicateWarning, RejectionReasonCode } from '../types/review.types';

interface ReviewQuestionCardProps {
  question: SessionQuestionReviewDto;
  questionNumber: number;
  duplicateWarnings: DuplicateWarning[];
  onApprove: (questionId: number) => void;
  onReject: (questionId: number, code: RejectionReasonCode, reason?: string) => Promise<void>;
  onEdit: (question: SessionQuestionReviewDto) => void;
  readOnly: boolean;
  disabled: boolean;
}

export function ReviewQuestionCard({
  question, questionNumber, duplicateWarnings, onApprove, onReject, onEdit, readOnly, disabled,
}: ReviewQuestionCardProps) {
  const [preview, setPreview] = useState<{ url: string; label: string } | null>(null);
  const approved = question.status === 'APPROVED';
  const rejected = question.status === 'REJECTED';
  const StatusIcon = approved ? CircleCheck : rejected ? XCircle : Clock3;
  const id = `review-question-${question.questionId}`;

  function imageButton(url: string, label: string) {
    return (
      <button type="button" onClick={() => setPreview({ url, label })} aria-label={`Phóng to ${label}`}
        className="group relative block max-w-full overflow-hidden rounded-lg border border-border bg-background focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-ring">
        <img src={url} alt={label} loading="lazy" className="h-24 max-w-full object-contain" />
        <ZoomIn aria-hidden="true" className="absolute bottom-1 right-1 size-6 rounded bg-card p-1 group-hover:text-primary" />
      </button>
    );
  }

  return (
    <article aria-labelledby={id} className="space-y-4 rounded-2xl border border-border bg-card p-4 sm:p-5">
      <header className="flex flex-wrap items-center justify-between gap-2 border-b border-border pb-3">
        <div className="flex flex-wrap items-center gap-2">
          <h2 id={id} tabIndex={-1} className="scroll-mt-24 font-semibold focus-visible:outline-2 focus-visible:outline-ring">Câu {questionNumber}</h2>
          <span className="font-mono text-xs text-muted-foreground">#{question.questionId}</span>
          <Badge variant="outline" className={approved ? 'bg-success-soft text-success'
            : rejected ? 'bg-destructive/10 text-destructive' : 'bg-warning-soft text-warning'}>
            <StatusIcon aria-hidden="true" /> {approved ? 'Đã phê duyệt' : rejected ? 'Đã từ chối' : 'Chờ duyệt'}
          </Badge>
        </div>
        {duplicateWarnings.length > 0 && <DuplicateWarningBadge warnings={duplicateWarnings} />}
      </header>
      <p className="wrap-break-word whitespace-pre-wrap leading-relaxed">{question.content}</p>
      {!!question.imageUrls?.length && (
        <div className="flex flex-wrap gap-2">
          {question.imageUrls.map((url, index) => (
            <div key={`${url}-${index}`} className="min-w-0">{imageButton(url, `ảnh đề bài ${index + 1}`)}</div>
          ))}
        </div>
      )}
      <ul aria-label="Phương án trả lời" className="grid gap-2 sm:grid-cols-2">
        {question.options?.map((option) => (
          <li key={option.key} className={cn('flex min-w-0 items-start gap-2 rounded-xl border p-3 text-sm',
            option.isCorrect ? 'border-success/30 bg-success-soft' : 'border-border bg-surface-subtle')}>
            <span className="shrink-0 font-semibold">{option.key}.</span>
            <div className="min-w-0 space-y-2">
              <p className="wrap-break-word whitespace-pre-wrap">{option.text}</p>
              {option.mediaUrl && imageButton(option.mediaUrl, `ảnh đáp án ${option.key}`)}
              {option.isCorrect && <p className="flex items-center gap-1 text-xs font-semibold text-success"><Check aria-hidden="true" className="size-4" /> Đáp án đúng</p>}
            </div>
          </li>
        ))}
      </ul>
      {question.explanation && (
        <div className="rounded-xl border border-border bg-surface-subtle p-3 text-sm">
          <p className="font-semibold">Lời giải</p>
          <p className="mt-1 wrap-break-word whitespace-pre-wrap text-muted-foreground">{question.explanation}</p>
        </div>
      )}
      {rejected && (
        <div className="rounded-xl border border-destructive/25 bg-destructive/10 p-3 text-sm">
          <p className="font-semibold text-destructive">{getRejectionReasonLabel(question.rejectionCode)}</p>
          {question.rejectionReason && <p className="mt-1 wrap-break-word whitespace-pre-wrap">{question.rejectionReason}</p>}
        </div>
      )}
      {question.editLog && <p className="text-sm text-muted-foreground">Biên tập gần nhất · {question.editLog.actorName}</p>}
      {!readOnly && <footer className="flex flex-wrap justify-end gap-2 border-t border-border pt-3">
        <Button variant="outline" className="min-h-11" disabled={disabled} onClick={() => onEdit(question)}>
          <Pencil aria-hidden="true" /> Biên tập
        </Button>
        <RejectReasonPopover disabled={disabled} isRejected={rejected}
          rejectionCode={question.rejectionCode} rejectionReason={question.rejectionReason}
          onConfirm={(code, reason) => onReject(question.questionId, code, reason)} />
        {!approved && (
          <Button className="min-h-11" disabled={disabled} onClick={() => onApprove(question.questionId)}>
            <CircleCheck aria-hidden="true" /> Phê duyệt
          </Button>
        )}
      </footer>}
      <Dialog.Root open={preview !== null} onOpenChange={(open) => { if (!open) setPreview(null); }}>
        <Dialog.Portal>
          <Dialog.Backdrop className="fixed inset-0 z-50 bg-black/70" />
          <Dialog.Popup className="fixed inset-x-4 top-1/2 z-50 mx-auto max-w-4xl -translate-y-1/2 rounded-2xl border border-border bg-card p-4 shadow-xl">
            <div className="mb-2 flex items-center justify-between gap-2">
              <Dialog.Title className="font-semibold">Xem {preview?.label}</Dialog.Title>
              <Dialog.Close render={<Button variant="ghost" size="icon" aria-label="Đóng ảnh" />}><X aria-hidden="true" /></Dialog.Close>
            </div>
            {preview && <img src={preview.url} alt={preview.label} className="mx-auto max-h-[75dvh] max-w-full object-contain" />}
          </Dialog.Popup>
        </Dialog.Portal>
      </Dialog.Root>
    </article>
  );
}
