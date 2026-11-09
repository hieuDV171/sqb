import { useId, useState, type FormEvent } from 'react';
import { Dialog } from '@base-ui/react/dialog';
import { Loader2, X, XCircle } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Label } from '@/components/ui/label';
import { getRejectionReasonLabel } from '@/lib/rejectionReasons';
import type { RejectionReasonCode } from '../types/review.types';

const reasons: RejectionReasonCode[] = [
  'DUPLICATE', 'INCORRECT_ANSWER', 'AMBIGUOUS_OR_INCOMPLETE', 'OUT_OF_SCOPE',
  'LOW_QUALITY', 'UNSPECIFIED', 'OTHER',
];

interface RejectReasonPopoverProps {
  disabled?: boolean;
  isRejected: boolean;
  rejectionCode?: RejectionReasonCode | null;
  rejectionReason?: string | null;
  onConfirm: (code: RejectionReasonCode, reason?: string) => Promise<void>;
}

export function RejectReasonPopover({ disabled, isRejected, rejectionCode, rejectionReason, onConfirm }: RejectReasonPopoverProps) {
  const [open, setOpen] = useState(false);
  const [code, setCode] = useState<RejectionReasonCode | null>(null);
  const [reason, setReason] = useState('');
  const [saving, setSaving] = useState(false);
  const formId = useId();

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!code || saving || (code === 'OTHER' && !reason.trim())) return;
    setSaving(true);
    try {
      await onConfirm(code, reason.trim() || undefined);
      setOpen(false);
    } catch {
      // Keep the selected reason and note available for retry.
    } finally {
      setSaving(false);
    }
  }

  return (
    <Dialog.Root open={open} onOpenChange={(next) => {
      if (saving) return;
      if (next) {
        setCode(rejectionCode && reasons.includes(rejectionCode) ? rejectionCode : null);
        setReason(rejectionReason ?? '');
      }
      setOpen(next);
    }}>
      <Dialog.Trigger render={<Button variant="destructive" className="min-h-11" disabled={disabled} />}>
        <XCircle aria-hidden="true" /> {isRejected ? 'Đổi lý do' : 'Từ chối'}
      </Dialog.Trigger>
      <Dialog.Portal>
        <Dialog.Backdrop className="fixed inset-0 z-50 bg-black/60" />
        <Dialog.Popup className="fixed inset-x-4 top-1/2 z-50 mx-auto max-h-[calc(100dvh-2rem)] max-w-md -translate-y-1/2 space-y-4 overflow-y-auto rounded-2xl border border-border bg-card p-5 shadow-xl">
          <div className="flex items-start justify-between gap-2">
            <div>
              <Dialog.Title className="text-lg font-semibold">Lý do từ chối</Dialog.Title>
              <Dialog.Description className="text-sm text-muted-foreground">Lý do được hiển thị cho người nộp phiên.</Dialog.Description>
            </div>
            <Dialog.Close render={<Button size="icon" variant="ghost" aria-label="Đóng" disabled={saving} />}>
              <X aria-hidden="true" />
            </Dialog.Close>
          </div>
          <form onSubmit={submit} className="space-y-4">
            <fieldset className="space-y-1" disabled={saving}>
              <legend className="mb-1 text-sm font-medium">Chọn một lý do</legend>
              {reasons.map((value) => (
                <label key={value} className="flex min-h-11 cursor-pointer items-center gap-3 rounded-lg border border-border px-3 text-sm has-checked:border-academic has-checked:bg-academic-soft">
                  <input type="radio" name={formId} value={value} checked={code === value}
                    onChange={() => setCode(value)} className="size-4 accent-academic" />
                  {getRejectionReasonLabel(value)}
                </label>
              ))}
            </fieldset>
            <div className="space-y-2">
              <Label htmlFor={formId}>{code === 'OTHER' ? 'Lý do cụ thể (bắt buộc)' : 'Ghi chú thêm (tùy chọn)'}</Label>
              <textarea id={formId} rows={3} maxLength={1000} value={reason} disabled={saving}
                required={code === 'OTHER'} onChange={(event) => setReason(event.target.value)}
                className="w-full rounded-xl border border-input bg-background p-3 text-sm focus-visible:outline-2 focus-visible:outline-ring" />
            </div>
            <div className="flex flex-wrap justify-end gap-2">
              <Dialog.Close render={<Button variant="outline" className="min-h-11" disabled={saving} />}>Quay lại</Dialog.Close>
              <Button type="submit" variant="destructive" className="min-h-11"
                disabled={!code || saving || (code === 'OTHER' && !reason.trim())}>
                {saving && <Loader2 aria-hidden="true" className="motion-safe:animate-spin" />}
                {saving ? 'Đang lưu…' : 'Lưu lý do từ chối'}
              </Button>
            </div>
          </form>
        </Dialog.Popup>
      </Dialog.Portal>
    </Dialog.Root>
  );
}
