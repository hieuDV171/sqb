import { useState } from 'react';
import { Dialog } from '@base-ui/react/dialog';
import { KeyRound, Loader2, Lock, X } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { ErrorState } from '@/components/ui/error-state';
import { useProfile } from '@/features/profile/hooks/useProfile';
import { toast } from '@/stores/useToastStore';
import { useSetHiddenPin, useUnlockHiddenChat } from '../hooks/useChat';
import { useChatStore } from '../stores/useChatStore';

const onlyPinDigits = (value: string) => value.replace(/[^0-9]/g, '').slice(0, 6);

function PinField({ label, value, onChange, disabled, autoFocus }: {
  label: string;
  value: string;
  onChange: (value: string) => void;
  disabled: boolean;
  autoFocus: boolean;
}) {
  return (
    <label className="block space-y-1.5 text-sm font-medium">
      <span>{label}</span>
      <input
        type="password"
        inputMode="numeric"
        pattern="[0-9]{6}"
        maxLength={6}
        autoComplete="off"
        autoFocus={autoFocus}
        required
        disabled={disabled}
        placeholder="Nhập 6 chữ số"
        value={value}
        onChange={(event) => onChange(onlyPinDigits(event.target.value))}
        className="block min-h-11 w-full rounded-xl border border-input bg-background px-3 text-center font-mono text-base tracking-wider focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-ring disabled:opacity-60"
      />
    </label>
  );
}

function HiddenChatPinForm({ configured, onClose }: { configured: boolean; onClose: () => void }) {
  const [changing, setChanging] = useState(false);
  const [pin, setPin] = useState('');
  const [oldPin, setOldPin] = useState('');
  const unlockPin = useChatStore((state) => state.unlockPin);
  const unlock = useUnlockHiddenChat();
  const save = useSetHiddenPin();
  const unlocking = configured && !changing;
  const busy = unlock.isPending || save.isPending;
  const Icon = unlocking ? Lock : KeyRound;

  const openVault = () => {
    unlockPin();
    onClose();
  };

  return (
    <>
      <header className="space-y-2 pr-9">
        <span className="mb-3 grid size-11 place-items-center rounded-xl bg-warning/15 text-warning">
          <Icon aria-hidden="true" className="size-5" />
        </span>
        <Dialog.Title className="text-xl font-semibold">
          {unlocking ? 'Mở khóa kho ẩn' : changing ? 'Đổi mã PIN kho ẩn' : 'Thiết lập mã PIN kho ẩn'}
        </Dialog.Title>
        <Dialog.Description className="text-sm text-muted-foreground">
          {unlocking ? 'Nhập mã PIN 6 chữ số để mở kho trò chuyện ẩn.' : 'Chọn mã PIN mới gồm đúng 6 chữ số.'}
        </Dialog.Description>
      </header>
      <form className="mt-5 space-y-4" onSubmit={(event) => {
        event.preventDefault();
        if (busy || pin.length !== 6 || (changing && oldPin.length !== 6)) return;
        if (unlocking) {
          unlock.mutate({ pin }, {
            onSuccess: () => {
              toast.success('Đã mở khóa kho trò chuyện ẩn');
              openVault();
            },
          });
        } else {
          save.mutate({ pin, ...(changing ? { oldPin } : {}) }, { onSuccess: openVault });
        }
      }}>
        {changing && <PinField label="Mã PIN cũ" value={oldPin} onChange={setOldPin} disabled={busy} autoFocus />}
        <PinField key={unlocking ? 'unlock' : 'new'} label={unlocking ? 'Mã PIN' : 'Mã PIN mới'} value={pin}
          onChange={setPin} disabled={busy} autoFocus={!changing} />
        <Button type="submit" className="min-h-11 w-full" disabled={busy || pin.length !== 6 || (changing && oldPin.length !== 6)}>
          {busy && <Loader2 aria-hidden="true" className="motion-safe:animate-spin" />}
          {unlocking ? 'Mở khóa' : 'Lưu mã PIN mới'}
        </Button>
        {configured && (
          <Button type="button" variant="ghost" className="min-h-11 w-full" disabled={busy} onClick={() => {
            setChanging(!changing);
            setPin('');
            setOldPin('');
          }}>
            {changing ? 'Quay lại mở khóa' : 'Đổi mã PIN'}
          </Button>
        )}
      </form>
    </>
  );
}

export function HiddenChatPinModal({ isOpen, onClose }: { isOpen: boolean; onClose: () => void }) {
  const profile = useProfile('me', isOpen);
  const configured = profile.data?.hiddenChatPinConfigured;

  return (
    <Dialog.Root open={isOpen} onOpenChange={(open) => { if (!open) onClose(); }}>
      <Dialog.Portal>
        <Dialog.Backdrop className="fixed inset-0 z-[70] bg-black/55" />
        <Dialog.Popup className="fixed top-1/2 left-1/2 z-[71] max-h-[calc(100dvh-2rem)] w-[calc(100%-2rem)] max-w-sm -translate-x-1/2 -translate-y-1/2 overflow-y-auto rounded-2xl border border-border bg-card p-5 text-foreground shadow-xl sm:p-6">
          {isOpen && (typeof configured === 'boolean' ? (
            <HiddenChatPinForm configured={configured} onClose={onClose} />
          ) : (
            <>
              <Dialog.Title className="pr-9 text-lg font-semibold">Mã PIN kho ẩn</Dialog.Title>
              {profile.isFetching ? (
                <p role="status" className="mt-4 text-sm text-muted-foreground">Đang kiểm tra trạng thái mã PIN…</p>
              ) : (
                <ErrorState title="Không thể kiểm tra trạng thái mã PIN" onRetry={() => void profile.refetch()} />
              )}
            </>
          ))}
          <Dialog.Close
            render={<Button variant="ghost" size="icon" className="absolute top-3 right-3 size-11" />}
            aria-label="Đóng hộp thoại mã PIN"
          >
            <X aria-hidden="true" />
          </Dialog.Close>
        </Dialog.Popup>
      </Dialog.Portal>
    </Dialog.Root>
  );
}
